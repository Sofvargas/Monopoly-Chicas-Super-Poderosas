# Diagrama UML — Monopoly Chicas Superpoderosas

Diagramas de clases del proyecto, separados por paquete para que se puedan leer.
Están escritos en [Mermaid](https://mermaid.js.org/): GitHub los dibuja solo al abrir este archivo.
En VS Code hace falta la extensión *Markdown Preview Mermaid Support* para verlos en la vista previa.

**Notación**

| Símbolo | Significado |
|---|---|
| `+` / `-` / `#` | público / privado / protegido |
| `$` (subrayado) | estático |
| `*` (cursiva) | abstracto |
| `<|--` | herencia |
| `*--` | composición (la parte no existe sin el todo) |
| `o--` | agregación (el todo guarda referencias a la parte) |
| `-->` | asociación (tiene una referencia) |
| sin símbolo | visible solo dentro del paquete |
| `..>` | dependencia (lo usa, pero no lo guarda) |

---

## 1. Vista general

Cómo se relacionan los paquetes y el hardware. Cada computadora corre la misma aplicación;
la del organizador además crea el servidor (el banco) y se conecta a la Raspberry Pi Pico W.

```mermaid
flowchart LR
    subgraph PC["Cada computadora"]
        UI["monopoly_proyect<br/>App · PantallaInicio · Interfaz"]
        CLI["client<br/>GameClient"]
    end

    subgraph ORG["Solo en la computadora del organizador"]
        SRV["server<br/>BankServer · ClientHandler<br/>GameSession · HardwareLink"]
        MOD["models<br/>Player · Square · Tarjeta<br/>EventCard · Transaction"]
        EST["structures<br/>listas y colas del grupo"]
    end

    PICO["Raspberry Pi Pico W<br/>dados · lector RFID"]

    UI -- "envía comandos" --> CLI
    CLI -- "TCP: una línea de texto por mensaje" --> SRV
    SRV -- "avisa a todas las pantallas" --> CLI
    CLI -- "mensajes recibidos" --> UI
    SRV --> MOD
    MOD --> EST
    SRV --> EST
    PICO -- "TCP: DADOS,d1,d2 · TARJETA,UID" --> SRV
```

---

## 2. Paquete `structures` — estructuras de datos propias

Las cinco estructuras del grupo y sus dos tipos de nodo. Todas son genéricas.

```mermaid
classDiagram
    direction LR

    class node~T~ {
        -T data
        -node~T~ next
        +getData() T
        +setData(T data)
        +getNext() node~T~
        +setNext(node~T~ next)
    }

    class DoubleNode~T~ {
        -T data
        -DoubleNode~T~ next
        -DoubleNode~T~ prev
        +getData() T
        +setData(T data)
        +getNext() DoubleNode~T~
        +setNext(DoubleNode~T~ next)
        +getPrev() DoubleNode~T~
        +setPrev(DoubleNode~T~ prev)
    }

    class SinglyLinkedList~T~ {
        -node~T~ head
        +add(T data)
        +removeProperty(T data)
        +getHead() node~T~
    }

    class CircularQueue~T~ {
        -node~T~ front
        -node~T~ rear
        -node~T~ currentTurn
        +addPlayer(T player)
        +getCurrentTurn() T
        +advanceTurn()
    }

    class ReusableQueue~T~ {
        -node~T~ front
        -node~T~ rear
        +enqueue(T card)
        +dequeue() T
        +drawAndReuse() T
    }

    class DoublyLinkedList~T~ {
        -DoubleNode~T~ head
        -DoubleNode~T~ tail
        +addTransaction(T transaction)
        +getOldest() DoubleNode~T~
        +getNewest() DoubleNode~T~
    }

    class CircularDoublyLinkedList~T~ {
        -DoubleNode~T~ head
        -int size
        +insert(T data)
        +movePositions(DoubleNode~T~ node, int positions) DoubleNode~T~
        +getSize() int
        +getHead() DoubleNode~T~
    }

    SinglyLinkedList *-- node : nodos
    CircularQueue *-- node : nodos
    ReusableQueue *-- node : nodos
    DoublyLinkedList *-- DoubleNode : nodos
    CircularDoublyLinkedList *-- DoubleNode : nodos
    node --> node : next
    DoubleNode --> DoubleNode : next / prev
```

| Estructura | Para qué se usa en el juego |
|---|---|
| `CircularQueue<Player>` | Turnos: el último jugador apunta al primero y el turno rota sin fin. |
| `CircularDoublyLinkedList<Square>` | Tablero: 24 casillas en círculo; la ficha avanza de nodo en nodo. |
| `DoublyLinkedList<Transaction>` | Historial de transacciones, de la más antigua a la más nueva. |
| `ReusableQueue<EventCard>` | Mazo de cartas sorpresa: la carta que sale vuelve al final. |
| `SinglyLinkedList<Property>` | Propiedades de cada jugador. |
| `SinglyLinkedList<Tarjeta>` | Tarjetas RFID por repartir al iniciar la partida. |

---

## 3. Paquete `models` — los datos del juego

```mermaid
classDiagram
    direction TB

    class Square {
        <<abstract>>
        #String id
        #String name
        +getId() String
        +getName() String
        +executeAction(Player player)*
    }

    class Property {
        -double purchasePrice
        -double rentPrice
        -Player owner
        +getPurchasePrice() double
        +getRentPrice() double
        +getOwner() Player
        +setOwner(Player owner)
        +executeAction(Player player)
    }

    class SpecialSquare {
        -String specialActionType
        +getSpecialActionType() String
        +executeAction(Player player)
    }

    class EventSquare {
        +executeAction(Player player)
    }

    class Player {
        -String id
        -String name
        -double balance
        -int currentPositionIndex
        -boolean isActive
        -int turnosEnCarcel
        -Tarjeta card
        -SinglyLinkedList~Property~ ownedProperties
        +getId() String
        +getName() String
        +getBalance() double
        +setBalance(double balance)
        +getCurrentPositionIndex() int
        +setCurrentPositionIndex(int index)
        +isActive() boolean
        +setActive(boolean active)
        +getTurnosEnCarcel() int
        +setTurnosEnCarcel(int turnos)
        +getCard() Tarjeta
        +setCard(Tarjeta card)
        +getOwnedProperties() SinglyLinkedList~Property~
        +addProperty(Property property)
        +calculateNetWorth() double
    }

    class Tarjeta {
        -String uid
        -String alias
        +getUid() String
        +getAlias() String
    }

    class EventCard {
        -String id
        -String description
        -String effectType
        -int value
        +getId() String
        +getDescription() String
        +getEffectType() String
        +getValue() int
    }

    class Transaction {
        -String id
        -LocalDateTime dateTime
        -int turnNumber
        -String type
        -String sourcePlayerId
        -String destinationPlayerId
        -double amount
        -String description
        +getId() String
        +getDateTime() LocalDateTime
        +getTurnNumber() int
        +getType() String
        +getSourcePlayerId() String
        +getDestinationPlayerId() String
        +getAmount() double
        +getDescription() String
        +toString() String
    }

    class Tablero {
        <<utility>>
        +String[] NOMBRES$
        +double SALARIO_POR_VUELTA$
        +int TURNOS_EN_CARCEL$
        +crear()$ CircularDoublyLinkedList~Square~
    }

    class Tarjetas {
        <<utility>>
        +String[] UIDS$
        +String[] ALIAS$
        +String[] IMAGENES$
        +crear()$ SinglyLinkedList~Tarjeta~
        +aliasDe(String uid)$ String
        +imagenDe(String alias)$ String
    }

    class Cartas {
        <<utility>>
        -todas()$ EventCard[]
        +crear()$ ReusableQueue~EventCard~
    }

    Square <|-- Property
    Square <|-- SpecialSquare
    Square <|-- EventSquare

    Property --> "0..1" Player : owner
    Player o-- "0..*" Property : ownedProperties
    Player --> "0..1" Tarjeta : card
    Square ..> Player : executeAction

    Tablero ..> Square : crea las 24 casillas
    Tarjetas ..> Tarjeta : crea las 4 tarjetas
    Cartas ..> EventCard : crea y baraja el mazo
```

- **`Square`** es abstracta: cada tipo de casilla redefine `executeAction` (polimorfismo).
  - `Property`: se puede comprar y cobra alquiler.
  - `SpecialSquare`: Inicio, Cárcel, Ir a la cárcel, Estacionamiento y Banco.
  - `EventSquare`: "Carta sorpresa" y "Sorpresa"; el jugador saca una carta.
- **`Tarjeta`** solo identifica al jugador; el saldo vive en `Player`, en el servidor.
- **`Transaction.type`** puede ser `PROPERTY_PURCHASE`, `RENT_PAYMENT`, `SALARY`, `EVENT_GAIN`, `EVENT_LOSS` o `BANKRUPTCY`.
- **`EventCard.effectType`** puede ser `RECEIVE_MONEY`, `PAY_MONEY` o `MOVE_FORWARD`.

---

## 4. Paquete `server` — el banco

El servidor es el único dueño del estado oficial de la partida. `GameSession` tiene las reglas;
las otras tres clases son la comunicación.

```mermaid
classDiagram
    direction TB

    class BankServer {
        +int DEFAULT_PORT$
        -CopyOnWriteArrayList~ClientHandler~ connectedClients$
        -GameSession session$
        -HardwareLink hardwareLink$
        +main(String[] args)$
        +startInBackground(int port)$
        -open(int port)$ ServerSocket
        -acceptLoop(ServerSocket serverSocket)$
        +broadcast(String message)$
        removeClient(ClientHandler client)$
        session()$ GameSession
        +hardwareRolled(int d1, int d2)$
        +hardwareCard(String uid)$
        +startHardware(String host, int port)$
        +stopHardware()$
        -announceRoll(String player, int d1, int d2)$
        +localAddresses()$ String
    }

    class ClientHandler {
        <<Runnable>>
        -Socket socket
        -BufferedReader in
        -PrintWriter out
        -CopyOnWriteArrayList~String~ localPlayers
        +run()
        -processCommand(String line)
        -handleConnect(String name)
        -handleStart(String player)
        -handleEndTurn(String player)
        -handleEndGame(String player)
        -sendState()
        -owns(String player) boolean
        +send(String message)
        -close()
    }

    class HardwareLink {
        <<Runnable>>
        -String host
        -int port
        -boolean running
        -Socket socket
        +start(String host, int port)$ HardwareLink
        +stop()
        +run()
        processLine(String line)
    }

    class GameSession {
        +String OK$
        +int MAX_PLAYERS$
        +double SALDO_INICIAL$
        -Player[] players
        -int count
        -CircularQueue~Player~ turns
        -CircularDoublyLinkedList~Square~ board
        -DoublyLinkedList~Transaction~ transactions
        -ReusableQueue~EventCard~ eventCards
        -int turnNumber
        -int transactionCounter
        -boolean started
        -boolean finished
        -boolean diceRolled
        -Player deudor
        -Property propiedadPendiente
        -StringBuilder avisos
        +addPlayer(String name) String
        +isRegistered(String name) boolean
        +start() String
        +isStarted() boolean
        +currentPlayer() String
        +playersAsText() String
        +cardsAsLines() String[]
        +tryRoll(String requester) String
        +applyRoll(int d1, int d2) String[]
        +cardTapped(String uid) String[]
        +endTurn(String name) String
        +takeNotices() String[]
        +endGame() String
        +historyAsLines() String[]
        +getTransactions() DoublyLinkedList~Transaction~
        -repartirTarjetas()
        -mover(Player p, int total, StringBuilder out)
        -resolverPropiedad(Player p, Property prop, StringBuilder out)
        -resolverCarcel(Player p, SpecialSquare square, StringBuilder out)
        -resolverCarta(Player p, StringBuilder out)
        -comprar(StringBuilder out)
        -pagarAlquiler(StringBuilder out)
        -eliminar(Player p, Player acreedor, String motivo, StringBuilder out)
        -avanzarTurno()
        -unicoActivo() Player
        -registrar(String type, String source, String destination, double amount, String description)
    }

    class CircularQueue~T~
    class CircularDoublyLinkedList~T~
    class DoublyLinkedList~T~
    class ReusableQueue~T~
    class Player
    class Square
    class Transaction
    class EventCard
    class Tablero
    class Tarjetas
    class Cartas

    BankServer "1" *-- "1" GameSession : session
    BankServer "1" o-- "0..*" ClientHandler : connectedClients
    BankServer "1" o-- "0..1" HardwareLink : hardwareLink
    ClientHandler ..> BankServer : session() / broadcast()
    HardwareLink ..> BankServer : hardwareRolled() / hardwareCard()

    GameSession *-- CircularQueue : turns
    GameSession *-- CircularDoublyLinkedList : board
    GameSession *-- DoublyLinkedList : transactions
    GameSession *-- ReusableQueue : eventCards
    GameSession "1" o-- "2..4" Player : players
    GameSession ..> Square : resuelve la casilla
    GameSession ..> Transaction : registra
    GameSession ..> EventCard : saca del mazo
    GameSession ..> Tablero : crear()
    GameSession ..> Tarjetas : crear()
    GameSession ..> Cartas : crear()
```

- **`BankServer`** acepta las conexiones y reenvía los mensajes a todas las pantallas (`broadcast`). Es todo estático: hay un solo banco por partida.
- **`ClientHandler`** atiende una computadora en su propio hilo. Traduce cada línea de texto en una llamada a `GameSession`.
- **`HardwareLink`** se conecta a la Pico en otro hilo y entrega los dados y las tarjetas a `BankServer`.
- **`GameSession`** aplica las reglas. Todos sus métodos públicos son `synchronized`, porque los llaman varios hilos a la vez.

> `Bank.java` también está en este paquete, pero es una versión anterior que el servidor no usa;
> su lógica (compra, alquiler, cartas, quiebra) se trasladó a `GameSession`. Por eso no aparece en el diagrama.

---

## 5. Paquetes `client` y `monopoly_proyect` — la conexión y la pantalla

```mermaid
classDiagram
    direction TB

    class Application {
        <<JavaFX>>
    }

    class App {
        -GameClient client
        +start(Stage stage)
        +stop()
        +main(String[] args)$
        -conectar(Stage stage, DatosConexion datos) String
        -mostrarJuego(Stage stage, DatosConexion datos)
        -escalarTexto(Scene scene, Parent raiz)
    }

    class PantallaInicio {
        -StackPane root
        -TextField campoNombre
        -TextField campoNombre2
        -TextField campoIp
        -TextField campoPuerto
        -TextField campoIpPico
        -TextField campoPuertoPico
        -CheckBox chkOrganizador
        -Label mensajeError
        +PantallaInicio(Function alConectar)
        +getRoot() Parent
        -intentarConectar(Function alConectar)
        -nombreValido(String nombre) boolean
    }

    class DatosConexion {
        <<record>>
        +String nombre
        +String nombre2
        +String ip
        +int puerto
        +boolean organizador
        +String ipPico
        +int puertoPico
    }

    class Interfaz {
        -BorderPane root
        -GridPane tablero
        -Label lblTurno
        -Label lblDados
        -Button btnIniciar
        -Button btnTerminar
        -Button btnTerminarPartida
        -VBox listaJugadores
        -TextArea registro
        -FlowPane[] zonasFichas
        -Map fichas
        -ObservableList historial
        -Consumer~String~ enviarComando
        -String[] jugadoresLocales
        -String turnoActual
        +getRoot() Parent
        +conectarRed(Consumer enviar, String[] locales)
        +procesarMensaje(String linea)
        +mostrarMensaje(String texto)
        -construirTablero()
        -crearCasilla(String texto, int indice) StackPane
        -posicion(int i) int[]
        -crearFicha(String jugador, String personaje)
        -moverFicha(String jugador, String indice)
        -siguientePaso(Ficha f)
        -eliminarFicha(String jugador)
        -resaltarTurno()
        -mostrarDados(String[] p, String linea)
        -pedirTerminarPartida()
        -agregarTransaccion(String[] p)
        -mostrarHistorial()
    }

    class Ficha {
        <<clase interna>>
        Node nodo
        HBox fila
        Label lblSaldo
        int casilla
        int destino
        ArrayDeque pendientes
        boolean animando
        boolean eliminada
    }

    class GameClient {
        -Socket socket
        -BufferedReader in
        -PrintWriter out
        -Consumer~String~ onMessage
        -Runnable onDisconnect
        -boolean closing
        +connect(String host, int port)
        +setOnMessage(Consumer handler)
        +setOnDisconnect(Runnable handler)
        +startListening()
        +send(String command)
        +isConnected() boolean
        +close()
        -listen()
    }

    class ConsoleClient {
        +main(String[] args)$
    }

    class BankServer
    class Tablero
    class Tarjetas

    Application <|-- App
    App --> "0..1" GameClient : client
    App ..> PantallaInicio : crea
    App ..> Interfaz : crea
    App ..> BankServer : solo el organizador lo inicia
    PantallaInicio *-- DatosConexion
    PantallaInicio ..> App : alConectar(datos)
    Interfaz *-- "0..4" Ficha : fichas
    Interfaz ..> GameClient : enviarComando = client.send
    GameClient ..> Interfaz : onMessage = procesarMensaje
    Interfaz ..> Tablero : NOMBRES
    Interfaz ..> Tarjetas : imagenDe()
    ConsoleClient ..> GameClient : pruebas por consola
```

- **`App`** arranca la aplicación, muestra `PantallaInicio` y, al conectar, la cambia por `Interfaz`.
- **`Interfaz`** no conoce a `GameClient`: solo recibe una función para enviar comandos. Así la pantalla y la red quedan separadas.
- **`GameClient`** no conoce JavaFX: entrega cada mensaje a la función que le pasen. `App` la envuelve en `Platform.runLater` para dibujar en el hilo de la pantalla.
- **`Ficha`** guarda lo que la pantalla sabe de un jugador: su personaje en el tablero y su fila en la lista de jugadores.

---

## 6. Protocolo de red

Los mensajes son líneas de texto con los campos separados por coma.

**De la pantalla al servidor**

| Comando | Qué hace |
|---|---|
| `CONECTAR,jugador` | Registra un jugador en esa computadora. |
| `INICIAR,jugador` | Inicia la partida (mínimo 2 jugadores) y reparte las tarjetas. |
| `TERMINAR_TURNO,jugador` | Pasa el turno. |
| `TERMINAR_PARTIDA,jugador` | Termina la partida para todos y pide el historial. |
| `CONSULTAR_ESTADO` | Pide la lista de jugadores y el turno. |

**De la Pico al servidor**

| Mensaje | Qué significa |
|---|---|
| `HOLA,PICO` | La Pico aceptó la conexión. |
| `DADOS,d1,d2` | Resultado de los dados físicos. |
| `TARJETA,UID` | El lector RFID leyó una tarjeta. |

**Del servidor a las pantallas**

| Mensaje | Qué significa |
|---|---|
| `OK,CONECTADO,jugador` / `ERROR,codigo` | Respuesta solo para quien envió el comando. |
| `JUGADORES,a;b;c` | Lista de jugadores registrados. |
| `INICIADA,a;b;c` | La partida comenzó. |
| `TARJETA_ASIGNADA,jugador,alias` | Tarjeta (y personaje) que le tocó a cada jugador. |
| `TURNO,jugador` | De quién es el turno. |
| `DADOS,jugador,d1,d2` | Lo que sacó el jugador en turno. |
| `POSICION,jugador,indice,casilla` | A dónde llegó la ficha. |
| `SALDO,jugador,saldo` | Saldo nuevo de un jugador. |
| `PROPIEDAD,jugador,indice,casilla` | El jugador compró esa casilla. |
| `CARTA,jugador,descripcion` | Carta sorpresa que sacó el jugador. |
| `ELIMINADO,jugador` | No pudo pagar y sale del juego. |
| `GANADOR,jugador` | Solo queda un jugador. |
| `HISTORIAL_INICIO,jugador` → `TRANSACCION,...` → `HISTORIAL_FIN` | Historial de transacciones al terminar la partida. |
| `MENSAJE,texto` | Aviso informativo. |

---

## 7. Secuencia de un turno

Ejemplo: Ana cae en una propiedad libre y la compra con su tarjeta.

```mermaid
sequenceDiagram
    actor Ana
    participant Pico as Pico W
    participant HL as HardwareLink
    participant BS as BankServer
    participant GS as GameSession
    participant UI as Interfaz (todas las pantallas)

    Ana->>Pico: pulsa el botón de los dados
    Pico->>HL: DADOS,1,2
    HL->>BS: hardwareRolled(1, 2)
    BS->>GS: tryRoll(null)
    GS-->>BS: OK
    BS->>UI: DADOS,Ana,1,2
    BS->>GS: applyRoll(1, 2)
    Note over GS: mueve la ficha por el tablero circular,<br/>paga el salario si pasa por Inicio<br/>y deja la compra pendiente
    GS-->>BS: POSICION · SALDO · MENSAJE
    BS->>UI: POSICION,Ana,3,Parque
    Note over UI: la ficha camina casilla por casilla

    Ana->>Pico: acerca su tarjeta
    Pico->>HL: TARJETA,61809517
    HL->>BS: hardwareCard(uid)
    BS->>GS: cardTapped(uid)
    Note over GS: verifica que la tarjeta sea de Ana,<br/>descuenta el precio, asigna la propiedad<br/>y registra la Transaction
    GS-->>BS: PROPIEDAD · SALDO
    BS->>UI: PROPIEDAD,Ana,3,Parque / SALDO,Ana,1410

    Ana->>UI: pulsa "Terminar turno"
    UI->>BS: TERMINAR_TURNO,Ana
    BS->>GS: endTurn("Ana")
    Note over GS: avanza la cola circular de turnos,<br/>saltando eliminados y encarcelados
    GS-->>BS: OK
    BS->>UI: TURNO,Beto
```
