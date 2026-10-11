# Manual Breve de Ejecución

**Monopoly – Chicas Superpoderosas** · Proyecto 1 de Algoritmos y Estructuras de Datos I (CE-1103)

Monopoly electrónico para 2 a 4 jugadores, en dos o más computadoras de la misma red.
Los dados y los pagos se hacen con un módulo físico (Raspberry Pi Pico W con botón, dos displays y lector RFID).

## 1. Requisitos

| Qué | Detalle |
|---|---|
| Java (JDK) | Versión 21 o superior, en todas las computadoras. Se comprueba con `java -version`. |
| Maven | No hay que instalarlo: el proyecto trae su propio lanzador (`mvnw`). La primera ejecución necesita internet para descargar las dependencias. |
| Red | Todas las computadoras y la Pico conectadas a la **misma red Wi-Fi** (de 2.4 GHz, la Pico W no usa 5 GHz). |
| Hardware | Solo en la computadora del organizador: el módulo con la Pico W encendido (ver sección 5). |

## 2. Ejecutar la aplicación

Descargar el proyecto y abrir una terminal en su carpeta:

```bash
git clone https://github.com/Sofvargas/Monopoly-Chicas-Super-Poderosas.git
cd Monopoly-Chicas-Super-Poderosas
```

**Windows** (PowerShell o CMD):

```powershell
.\mvnw.cmd clean javafx:run
```

**macOS** (Terminal):

```bash
./mvnw clean javafx:run
```

Si macOS responde `permission denied`, dar permiso una sola vez con `chmod +x mvnw` y repetir el comando.

Se abre la ventana de inicio. El mismo comando se usa en todas las computadoras: la del organizador y las de los demás jugadores.

## 3. Crear la partida y unirse

Hay que seguir este orden. Una vez iniciada la partida no se puede unir nadie más.

**Paso 1 — El organizador** (la computadora que tiene el hardware):

1. Encender la Pico y anotar la IP que muestra (sección 5).
2. Escribir su nombre. Si otra persona juega en la misma computadora, escribir su nombre en "Segundo jugador".
3. Marcar **"Soy el organizador (crear la partida)"**.
4. Escribir la **IP de la Pico** y su puerto (8080). Sin la Pico no se puede jugar: los dados solo llegan del hardware.
5. Pulsar **Conectar**. En el registro aparece: *"Los demás deben conectarse a: \<IP\> (puerto 8080)"*.

**Paso 2 — Los demás jugadores** (cada uno en su computadora):

1. Escribir su nombre (distinto al de los otros jugadores).
2. **No** marcar "Soy el organizador".
3. En "IP del servidor" escribir la IP que mostró la pantalla del organizador. Puerto: 8080.
4. Pulsar **Conectar**.

**Paso 3 — Iniciar:** cuando el registro muestre a todos en *"Jugadores conectados"*, cualquiera pulsa **Iniciar partida**.
A cada jugador se le asigna al azar una tarjeta RFID y su personaje (Bombón, Burbuja, Bellota o Mojo Jojo), con un saldo de $1500.

## 4. Cómo se juega

En cada turno, el jugador indicado en *"Turno de: ..."*:

1. **Tira los dados** pulsando el botón físico. El resultado sale en los displays y en todas las pantallas, y su ficha avanza.
2. Según la casilla donde cae:

   | Casilla | Qué pasa |
   |---|---|
   | Propiedad libre | Puede comprarla **acercando su tarjeta** al lector. Si no quiere, termina el turno. |
   | Propiedad de otro jugador | Debe pagar el alquiler **acercando su tarjeta**. No puede terminar el turno sin pagar. |
   | Carta sorpresa / Sorpresa | Saca una carta: cobra, paga o avanza casillas. Se aplica sola. |
   | Cárcel / Ir a la cárcel | Pierde sus dos turnos siguientes. |
   | Inicio | Al pasar por ella cobra $200. |

3. Pulsa **Terminar turno** en su pantalla.

Acercar una tarjeta cuando no hay nada que pagar muestra el saldo de su dueño.

**Fin de la partida.** La partida termina cuando:

- queda un solo jugador (los demás fueron eliminados por no poder pagar), o
- se completan 40 turnos; gana el de mayor patrimonio (saldo + valor de sus propiedades), o
- alguien pulsa **Terminar partida**.

El botón **Terminar partida** abre en todas las pantallas el historial de transacciones, con el botón **Salir del juego**.

## 5. Módulo electrónico (Raspberry Pi Pico W)

Los archivos están en la carpeta `Micropython/`.

1. Abrir `raspb_conection.py` y poner el nombre y la contraseña de la red Wi-Fi en `SSID` y `PASSWORD`.
2. Copiar a la Pico (por ejemplo con Thonny):
   - `mfrc.py`, guardado en la Pico con el nombre **`mfrc522.py`**
   - `raspb_conection.py`, guardado como `main.py` para que arranque solo al encender
3. Encender la Pico. Por la consola muestra: *"Java debe conectarse a \<IP\> puerto 8080"*. Esa es la IP que escribe el organizador.

**Conexiones**

| Componente | Pines de la Pico |
|---|---|
| Botón de los dados | GP19 (el otro lado a GND) |
| Displays de 7 segmentos, segmentos a–g | GP14, GP13, GP12, GP11, GP18, GP17, GP16 |
| Displays, selección de dígito | GP9 (dado 1), GP10 (dado 2) |
| Displays, punto decimal | GP15 |
| Lector RFID RC522 | SCK GP6 · MOSI GP7 · MISO GP4 · CS GP5 · RST GP22 · 3.3 V y GND |

Los displays son de ánodo común y cada dígito se activa con un transistor PNP.

## 6. Ajustes

| Qué cambiar | Dónde |
|---|---|
| Tarjetas RFID (UID), personaje e imagen de cada una | `src/main/java/models/Tarjetas.java` |
| Cantidad máxima de turnos, salario por vuelta, turnos en la cárcel, nombres de las casillas | `src/main/java/models/Tablero.java` |
| Cartas sorpresa | `src/main/java/models/Cartas.java` |
| Saldo inicial y máximo de jugadores | `src/main/java/server/GameSession.java` |

Para conocer el UID de una tarjeta nueva, acercarla al lector: el registro muestra *"Tarjeta desconocida: \<UID\>"*.

## 7. Problemas comunes

| Síntoma | Causa y solución |
|---|---|
| "No se pudo conectar a ..." | La IP no es la del organizador, el organizador aún no pulsó Conectar, las computadoras están en redes distintas o el firewall bloquea a Java. En Windows, permitir Java en redes privadas cuando lo pregunte. |
| "No se pudo crear la partida. ¿El puerto 8080 ya está en uso?" | Quedó otra ventana del juego abierta en esa computadora. Cerrarla o usar otro puerto (el mismo en todas). |
| Un jugador no aparece en "Jugadores conectados" | Marcó "Soy el organizador" por error (creó otra partida) o dejó la IP en `localhost`. Cerrar y volver a conectarse con la IP correcta. |
| `Error: PARTIDA_EN_CURSO` | Se conectó después de iniciar la partida. Hay que cerrar todas las ventanas y empezar de nuevo. |
| `Error: NOMBRE_DUPLICADO` | Ya hay un jugador con ese nombre. |
| "No se encuentra el hardware en ..." | La IP de la Pico es incorrecta, está apagada o en otra red. El juego reintenta cada 3 segundos. |
| "Tirada del hardware ignorada (DADOS_YA_LANZADOS)" | Ese jugador ya tiró en este turno: debe pulsar Terminar turno. |
| `Error: PAGO_PENDIENTE` | Debe pagar el alquiler con su tarjeta antes de terminar el turno. |
| "Pago rechazado: la tarjeta ... es de ..." | Se acercó la tarjeta de otro jugador. |

## Más información

El diseño del sistema (clases, estructuras de datos y protocolo de red) está en [UML.md](UML.md).
