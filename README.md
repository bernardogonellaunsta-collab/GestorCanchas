
# Gestor Deportivo (mínimo) — Solo clases del diagrama

Este proyecto implementa únicamente las clases visibles en el recuadro del diagrama: **GestorDeportivoApp, Complejo, Cancha, HorarioLaboral, Usuario, Cliente, Reserva (abstracta), ReservaSimple, ReservaFija**.  
No se agregan excepciones personalizadas ni otras clases auxiliares. La lógica de reserva y la persistencia básica (serialización) se centralizan en **Reserva** mediante métodos estáticos, tal como sugiere la nota del diagrama.

## Requisitos
- JDK 11+
- Sistema de archivos con permisos de escritura en la carpeta `data/` que se crea automáticamente junto al proyecto.

## Compilar y ejecutar (sin IDE)
```bash
cd src
javac com/gestor/*.java
java com.gestor.GestorDeportivoApp
```

## Notas de diseño
- Persistencia: las reservas se guardan en `data/reservas.dat`. Si el archivo no existe, se crea la carpeta `data` y se trabaja con lista vacía.
- Validaciones mínimas: para mantener el modelo acotado y sin agregar excepciones, si una nueva reserva **solapa** con otra en la misma cancha, simplemente **no se registra**.
- Disponibilidad: `Reserva.consultarDisponibilidad(idCancha, fecha)` genera turnos de 60 min entre 08:00 y 23:00 y descarta los que se superponen con reservas existentes para esa fecha y cancha.

## Estructura
```
src/com/gestor/*.java
data/
```

## Ejecución de ejemplo
El `main` crea una reserva simple de 60 minutos para hoy a las 19:00 en la Cancha 1 y luego imprime la disponibilidad para hoy. Vuelva a ejecutar para observar cómo cambia la disponibilidad.
