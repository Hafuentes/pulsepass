# Fixtures locales E2E

Esta carpeta contiene exclusivamente recursos para pruebas. Nunca ejecutes estas instrucciones contra producción ni contra una base distinta a pulsepass_e2e.

## Seed opcional de sede

seed-e2e.sql crea de forma idempotente una sola sede local con el código VEN-E2E-SEED. El script verifica current_database() antes del INSERT y aborta si la base no es exactamente pulsepass_e2e. No elimina datos, no modifica usuarios, no asigna roles y no escribe contraseñas ni tokens.

La corrida histórica usa VEN-E2E-R20261018A. El seed opcional no reemplaza ese dato ni debe cambiarse para reutilizar una corrida; usa un código nuevo para cada ejecución si la unicidad del entorno lo requiere.

## Cuentas privilegiadas locales

No asignes roles ADMIN o STAFF desde el endpoint de registro ni incluyas contraseñas en fixtures. El proyecto dispone del perfil Spring local-seed, que solo debe usarse en desarrollo y toma PULSEPASS_SEED_ADMIN_PASSWORD y PULSEPASS_SEED_STAFF_PASSWORD desde el entorno. Ambas contraseñas deben cumplir el mínimo configurado por la aplicación. Mantén las variables fuera de Git y verifica los roles en /api/auth/me.

Si el proyecto no está configurado para local-seed, aprovisiona ADMIN/STAFF mediante el procedimiento local ya aprobado del proyecto y deja constancia; no alteres cuentas ajenas a la corrida.
