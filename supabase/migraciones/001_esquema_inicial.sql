-- DonaConecta · Esquema inicial (Supabase / PostgreSQL)
-- Ejecutar en el SQL Editor de Supabase o con `supabase db push`.

-- ============ TIPOS ============
create type public.rol_usuario        as enum ('donante', 'empresa', 'fundacion');
create type public.condicion_objeto   as enum ('nuevo', 'muy_bueno', 'bueno', 'usado');
create type public.tipo_entrega       as enum ('retiro', 'punto_encuentro');
create type public.estado_donacion    as enum ('borrador', 'publicada', 'reservada', 'entregada', 'cancelada');
create type public.estado_solicitud   as enum ('pendiente', 'aceptada', 'rechazada');
create type public.angulo_foto        as enum ('frontal', 'trasera', 'detalle', 'extra');

-- ============ TABLAS ============
create table public.perfiles (
    id              uuid primary key references auth.users(id) on delete cascade,
    nombre_completo text not null default '',
    rol             public.rol_usuario not null default 'donante',
    creado_en       timestamptz not null default now()
);

create table public.fundaciones (
    id                   uuid primary key default gen_random_uuid(),
    perfil_id            uuid not null unique references public.perfiles(id) on delete cascade,
    nombre               text not null,
    ubicacion            text,
    categorias_aceptadas text[] not null default '{}',
    verificada           boolean not null default false,   -- solo la puede cambiar un administrador
    creado_en            timestamptz not null default now()
);

create table public.donaciones (
    id                   uuid primary key default gen_random_uuid(),
    donante_id           uuid not null references public.perfiles(id) on delete cascade,
    titulo               text not null check (char_length(titulo) between 3 and 120),
    categoria            text not null,
    descripcion          text not null default '',
    cantidad             integer not null default 1 check (cantidad > 0),
    unidad               text not null default 'unidad',
    condicion            public.condicion_objeto not null default 'nuevo',
    ubicacion_aproximada text,
    tipo_entrega         public.tipo_entrega not null default 'retiro',
    estado               public.estado_donacion not null default 'borrador',
    creado_en            timestamptz not null default now()
);
create index idx_donaciones_donante on public.donaciones(donante_id);
create index idx_donaciones_estado  on public.donaciones(estado);

create table public.fotos_donacion (
    id          uuid primary key default gen_random_uuid(),
    donacion_id uuid not null references public.donaciones(id) on delete cascade,
    angulo      public.angulo_foto not null,
    url         text not null,
    brillo      numeric(5,2),
    unique (donacion_id, angulo)
);

create table public.analisis_ia (
    id           uuid primary key default gen_random_uuid(),
    donacion_id  uuid not null unique references public.donaciones(id) on delete cascade,
    calidad_foto numeric(3,1) not null check (calidad_foto between 0 and 10),
    aprobado     boolean not null,
    motivos      jsonb not null default '[]',
    creado_en    timestamptz not null default now()
);

create table public.solicitudes_donacion (
    id           uuid primary key default gen_random_uuid(),
    donacion_id  uuid not null references public.donaciones(id) on delete cascade,
    fundacion_id uuid not null references public.fundaciones(id) on delete cascade,
    estado       public.estado_solicitud not null default 'pendiente',
    creado_en    timestamptz not null default now(),
    unique (donacion_id, fundacion_id)
);

-- ============ TRIGGERS ============
-- Crea el perfil al registrarse (rol y nombre vienen en los metadatos del signup).
create or replace function public.crear_perfil_al_registrarse()
returns trigger language plpgsql security definer set search_path = public as $$
begin
    insert into public.perfiles (id, nombre_completo, rol)
    values (
        new.id,
        coalesce(new.raw_user_meta_data->>'nombre_completo', ''),
        coalesce((new.raw_user_meta_data->>'rol')::public.rol_usuario, 'donante')
    );
    return new;
end $$;

create trigger trg_crear_perfil
after insert on auth.users
for each row execute function public.crear_perfil_al_registrarse();

-- Publica la donación SOLO si el análisis de IA fue aprobado (regla de negocio en el servidor).
create or replace function public.publicar_si_analisis_aprobado()
returns trigger language plpgsql security definer set search_path = public as $$
begin
    if new.aprobado then
        update public.donaciones set estado = 'publicada'
        where id = new.donacion_id and estado = 'borrador';
    end if;
    return new;
end $$;

create trigger trg_publicar_si_aprobado
after insert on public.analisis_ia
for each row execute function public.publicar_si_analisis_aprobado();

-- ============ PERMISOS POR COLUMNA (evita escalada de privilegios) ============
revoke insert, update on public.perfiles     from authenticated;
revoke insert, update on public.fundaciones  from authenticated;
revoke insert, update on public.donaciones   from authenticated;

grant update (nombre_completo) on public.perfiles to authenticated;
grant insert (perfil_id, nombre, ubicacion, categorias_aceptadas) on public.fundaciones to authenticated;
grant update (nombre, ubicacion, categorias_aceptadas)            on public.fundaciones to authenticated;
grant insert (donante_id, titulo, categoria, descripcion, cantidad, unidad, condicion, ubicacion_aproximada, tipo_entrega)
    on public.donaciones to authenticated;
grant update (titulo, descripcion, cantidad, unidad, condicion, ubicacion_aproximada, tipo_entrega)
    on public.donaciones to authenticated;

-- ============ ROW LEVEL SECURITY ============
alter table public.perfiles             enable row level security;
alter table public.fundaciones          enable row level security;
alter table public.donaciones           enable row level security;
alter table public.fotos_donacion       enable row level security;
alter table public.analisis_ia          enable row level security;
alter table public.solicitudes_donacion enable row level security;

create policy "perfil_propio_lectura"     on public.perfiles for select using (id = auth.uid());
create policy "perfil_propio_edicion"     on public.perfiles for update using (id = auth.uid());

create policy "fundaciones_lectura"       on public.fundaciones for select to authenticated using (true);
create policy "fundacion_propia_insertar" on public.fundaciones for insert to authenticated with check (perfil_id = auth.uid());
create policy "fundacion_propia_editar"   on public.fundaciones for update to authenticated using (perfil_id = auth.uid());

create policy "donaciones_lectura" on public.donaciones for select to authenticated using (
    donante_id = auth.uid()
    or (estado = 'publicada' and exists (select 1 from public.perfiles p where p.id = auth.uid() and p.rol = 'fundacion'))
);
create policy "donaciones_insertar" on public.donaciones for insert to authenticated with check (donante_id = auth.uid());
create policy "donaciones_editar"   on public.donaciones for update to authenticated using (donante_id = auth.uid());

-- Los subselects respetan el RLS de donaciones: ve fotos/análisis quien ve la donación.
create policy "fotos_lectura"   on public.fotos_donacion for select to authenticated
    using (exists (select 1 from public.donaciones d where d.id = donacion_id));
create policy "fotos_insertar"  on public.fotos_donacion for insert to authenticated
    with check (exists (select 1 from public.donaciones d where d.id = donacion_id and d.donante_id = auth.uid()));
create policy "analisis_lectura"  on public.analisis_ia for select to authenticated
    using (exists (select 1 from public.donaciones d where d.id = donacion_id));
create policy "analisis_insertar" on public.analisis_ia for insert to authenticated
    with check (exists (select 1 from public.donaciones d where d.id = donacion_id and d.donante_id = auth.uid()));

create policy "solicitudes_lectura" on public.solicitudes_donacion for select to authenticated using (
    exists (select 1 from public.fundaciones f where f.id = fundacion_id and f.perfil_id = auth.uid())
    or exists (select 1 from public.donaciones d where d.id = donacion_id and d.donante_id = auth.uid())
);
create policy "solicitudes_insertar" on public.solicitudes_donacion for insert to authenticated with check (
    exists (select 1 from public.fundaciones f where f.id = fundacion_id and f.perfil_id = auth.uid())
);

-- ============ STORAGE (fotos) ============
insert into storage.buckets (id, name, public) values ('fotos-donaciones', 'fotos-donaciones', true)
on conflict (id) do nothing;

create policy "fotos_subir_en_carpeta_propia" on storage.objects for insert to authenticated
with check (bucket_id = 'fotos-donaciones' and (storage.foldername(name))[1] = auth.uid()::text);
