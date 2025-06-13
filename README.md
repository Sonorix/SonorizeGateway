# GatewaySonorize - API Gateway for Microservices

Este proyecto implementa un API Gateway para los microservicios de Sonorize, permitiendo centralizar todas las peticiones API a través de un solo punto de entrada.

## Descripción General

GatewaySonorize actúa como intermediario entre los clientes y los microservicios, enrutando las peticiones al servicio correspondiente. Esto permite:

- Tener una sola URL base para todas las peticiones
- Mayor seguridad al ocultar la estructura interna de los microservicios
- Centralizar la lógica común como logging, autenticación, etc.
- Simplificar la arquitectura para los clientes

## Microservicios Soportados

Actualmente, el gateway da soporte a los siguientes microservicios:

1. **Users-ms** - Gestión de usuarios
   - Endpoints: `/login`, `/register`

2. **Contracts-payments-ms** - Gestión de contratos y pagos 
   - Endpoints: `/contract`, `/payment`

## Configuración

El gateway está configurado para conectarse a los microservicios en las siguientes URLs:

- Users-ms: `http://localhost:8081/Users-ms`
- Contracts-payments-ms: `http://localhost:8082/Contracts-payments-ms`

Si necesitas modificar estas URLs, puedes hacerlo en el archivo `ApiGatewayServlet.java`.

## Cómo Ejecutar Peticiones al Gateway

### URL Base

Todas las peticiones deben dirigirse a:

```
http://localhost:8080/GatewaySonorize/api/{endpoint}
```

Donde `{endpoint}` es el endpoint específico que deseas llamar.

### Endpoints Disponibles

#### 1. Gestión de Usuarios

**Registro de Usuario**

```
POST /api/register
```

Payload:
```json
{
  "nombre": "Ejemplo Nombre",
  "email": "ejemplo@email.com",
  "password": "password123",
  "tipoUsuario": "cliente",
  "telefono": "1234567890"
}
```

**Login**

```
POST /api/login
```

Payload:
```json
{
  "email": "ejemplo@email.com",
  "password": "password123"
}
```

#### 2. Gestión de Contratos y Pagos

**Crear Contrato**

```
POST /api/contract
```

Payload:
```json
{
  "idUsuario": "uuid-del-usuario",
  "idEvento": "uuid-del-evento",
  "idMusico": "uuid-del-musico",
  "pagoTotal": 1000.50,
  "estado": "Pendiente"
}
```

**Realizar Pago**

```
POST /api/payment
```

Payload:
```json
{
  "idContrato": "uuid-del-contrato",
  "metodoPago": "Tarjeta",
  "monto": 500.25,
  "estado": "Pendiente"
}
```

### Ejemplos con cURL

**Registrar un nuevo usuario:**

```bash
curl -X POST http://localhost:8080/GatewaySonorize/api/register \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Usuario Prueba",
    "email": "prueba@email.com",
    "password": "contraseña123",
    "tipoUsuario": "cliente",
    "telefono": "9876543210"
  }'
```

**Iniciar sesión:**

```bash
curl -X POST http://localhost:8080/GatewaySonorize/api/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "prueba@email.com",
    "password": "contraseña123"
  }'
```

**Crear un contrato:**

```bash
curl -X POST http://localhost:8080/GatewaySonorize/api/contract \
  -H "Content-Type: application/json" \
  -d '{
    "idUsuario": "11111111-1111-1111-1111-111111111111",
    "idEvento": "22222222-2222-2222-2222-222222222222",
    "idMusico": "33333333-3333-3333-3333-333333333333",
    "pagoTotal": 2000.00,
    "estado": "Pendiente"
  }'
```

**Realizar un pago:**

```bash
curl -X POST http://localhost:8080/GatewaySonorize/api/payment \
  -H "Content-Type: application/json" \
  -d '{
    "idContrato": "44444444-4444-4444-4444-444444444444",
    "metodoPago": "PayPal",
    "monto": 1000.00,
    "estado": "Procesando"
  }'
```

## Notas Técnicas

- El gateway espera y devuelve datos en formato JSON.
- Los errores son devueltos con el código HTTP apropiado y un mensaje explicativo.
- Todas las comunicaciones entre el gateway y los microservicios se registran en los logs para facilitar la depuración.

## Consideraciones para Producción

Para un entorno de producción, considera implementar:

1. SSL/TLS para todas las comunicaciones
2. Autenticación mediante JWT o similar
3. Rate limiting para prevenir abusos
4. Balanceo de carga si se requiere alta disponibilidad
5. Monitoreo y métricas de rendimiento

## Resolución de Problemas

Si encuentras problemas con el gateway, verifica:

1. Que todos los microservicios estén en ejecución
2. Que los puertos configurados sean correctos
3. Los logs del gateway para identificar posibles errores
4. Las respuestas de error del gateway para obtener más información
