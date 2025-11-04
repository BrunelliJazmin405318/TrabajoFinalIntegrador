-- Crea tabla de pagos manuales (seña/final)
create table if not exists pago_manual (
                                           id              bigserial primary key,
                                           presupuesto_id  bigint not null references presupuesto(id) on delete cascade,

    -- Tipo de pago manual: 'SENA' | 'FINAL'
    tipo            varchar(20) not null,

    -- Medio: 'EFECTIVO' | 'TRANSFERENCIA' | 'TARJETA' | 'OTRO'
    medio           varchar(30) not null,

    monto           numeric(12,2) not null check (monto >= 0),
    moneda          varchar(3) not null default 'ARS',

    observacion     text,
    creado_en       timestamp not null default now(),
    creado_por      varchar(80)
    );

-- Índices útiles
create index if not exists idx_pago_manual_presupuesto on pago_manual(presupuesto_id);
create index if not exists idx_pago_manual_tipo on pago_manual(tipo);