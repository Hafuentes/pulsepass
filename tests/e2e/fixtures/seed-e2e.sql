-- Exclusivo de pruebas E2E locales. No ejecutar en producción.
-- Protege el objetivo: no realiza ningún cambio si la DB no es pulsepass_e2e.
DO $$
BEGIN
    IF current_database() <> 'pulsepass_e2e' THEN
        RAISE EXCEPTION 'seed-e2e.sql solo puede ejecutarse en pulsepass_e2e; base actual: %', current_database();
    END IF;
END
$$;

-- Solo crea una sede de prueba. No borra ni altera usuarios, eventos o tickets.
INSERT INTO public.venues (code, name, city, address, capacity, active)
VALUES ('VEN-E2E-SEED', 'PulsePass E2E Venue', 'Bogotá', 'Dirección de pruebas', 500, TRUE)
ON CONFLICT (code) DO NOTHING;
