from machine import Pin, Timer
from time import ticks_ms, ticks_diff, ticks_add, ticks_us, sleep_ms, sleep
import random
import network
import socket
from mfrc522 import MFRC522

# ---------------- Configuración ----------------
SSID = "Mari"
PASSWORD = "Mari2016"
PORT = 8080              # puerto TCP donde Java se conecta a la Pico

BOTON_PIN = 19           # GP19 (pin físico 25); el otro lado del botón va a GND
SEG_ON = 0               # ánodo común
DIG_ON = 0               # transistores PNP

# ---------------- Display (se refresca solo con un Timer) ----------------
segmentos = [Pin(n, Pin.OUT) for n in (14, 13, 12, 11, 18, 17, 16)]  # a b c d e f g
digitos = [Pin(9, Pin.OUT), Pin(10, Pin.OUT)]                         # [0]=dado 1, [1]=dado 2
dp = Pin(15, Pin.OUT)
dp.value(1 - SEG_ON)     # punto siempre apagado

# 1 = segmento encendido (orden: a b c d e f g)
PATRONES = {
    "-": [0, 0, 0, 0, 0, 0, 1],
    1:   [0, 1, 1, 0, 0, 0, 0],
    2:   [1, 1, 0, 1, 1, 0, 1],
    3:   [1, 1, 1, 1, 0, 0, 1],
    4:   [0, 1, 1, 0, 0, 1, 1],
    5:   [1, 0, 1, 1, 0, 1, 1],
    6:   [1, 0, 1, 1, 1, 1, 1],
}

valores = ["-", "-"]     # lo que muestra cada dígito; el programa solo cambia esto
pos = 0

def refrescar_display(t):
    # Cada 4 ms muestra un dígito y alterna. Corre en segundo plano,
    # así el display no parpadea aunque el lector o la red tarden.
    global pos
    digitos[0].value(1 - DIG_ON)
    digitos[1].value(1 - DIG_ON)
    p = PATRONES[valores[pos]]
    for i in range(7):
        segmentos[i].value(SEG_ON if p[i] == 1 else 1 - SEG_ON)
    digitos[pos].value(DIG_ON)
    pos = 1 - pos

timer_display = Timer(-1)
timer_display.init(period=4, mode=Timer.PERIODIC, callback=refrescar_display)

# ---------------- Wi-Fi y servidor TCP ----------------
def Conexion_wifi():
    wlan = network.WLAN(network.STA_IF)
    wlan.active(True)
    # Desactiva el ahorro de energía del Wi-Fi: sin esto la Pico W no atiende
    # bien las conexiones entrantes (el servidor TCP no respondía a Java)
    try:
        wlan.config(pm=0xa11140)
    except Exception as e:
        print("No se pudo desactivar el ahorro de energía:", e)

    if not wlan.isconnected():
        print("Conectando a WiFi...")
        try:
            wlan.connect(SSID, PASSWORD)
        except Exception as e:
            print("Error inicializando WiFi:", e)
            return None

        timeout = 15
        while not wlan.isconnected() and timeout > 0:
            sleep(1)
            timeout -= 1
            print("Esperando conexión...", timeout)

    if wlan.isconnected():
        print("Conectado:", wlan.ifconfig())
        return wlan
    else:
        print("No hay conexión WiFi")
        return None

def Iniciar_server():
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    try:
        s.bind(("0.0.0.0", PORT))     # escucha en la IP que le dé el router
        s.listen(1)
        s.setblocking(False)          # accept() no se queda esperando
        print("Servidor TCP escuchando en puerto", PORT)
        return s
    except Exception as e:
        print("Error al iniciar servidor:", e)
        s.close()
        return None

cliente = None

def enviar(linea):
    # Manda una línea de texto a Java. Si no hay Java conectado, solo la imprime.
    global cliente
    print("->", linea)
    if cliente is None:
        return
    try:
        cliente.send((linea + "\n").encode("utf-8"))
    except OSError:
        print("Java se desconectó")
        try:
            cliente.close()
        except OSError:
            pass
        cliente = None

# ---------------- Botón (con interrupción, no se pierde ninguna pulsación) ----------------
boton = Pin(BOTON_PIN, Pin.IN, Pin.PULL_UP)
pulsado = False
t_boton = ticks_add(ticks_ms(), -1000)

def al_pulsar(pin):
    global pulsado, t_boton
    ahora = ticks_ms()
    if ticks_diff(ahora, t_boton) > 250:     # antirrebote
        t_boton = ahora
        pulsado = True

boton.irq(trigger=Pin.IRQ_FALLING, handler=al_pulsar)

# ---------------- Arranque ----------------
wlan = Conexion_wifi()
servidor = Iniciar_server() if wlan is not None else None
if servidor is not None:
    print(">>> Java debe conectarse a", wlan.ifconfig()[0], "puerto", PORT)
else:
    print("Sin red: los dados y el lector funcionan, pero no se envía nada a Java")

rfid = MFRC522(sck=6, mosi=7, miso=4, rst=22, cs=5, spi_id=0)
print("Versión del lector:", hex(rfid._rreg(0x37)))

animando = False
t_ini = 0
t_cambio = 0
ultimo_uid = ""
t_uid = 0

print("Listo: pulsá el botón para tirar los dados o acercá una tarjeta")

# ---------------- Programa principal ----------------
while True:
    # 1) ¿Java se conectó? (si ya había uno, se reemplaza por el nuevo)
    if servidor is not None:
        try:
            nuevo, addr = servidor.accept()
            if cliente is not None:
                try:
                    cliente.close()
                except OSError:
                    pass
            cliente = nuevo
            cliente.settimeout(2)
            print("Java conectado desde:", addr)
            enviar("HOLA,PICO")
        except OSError:
            pass    # nadie intentó conectarse

    # 2) Botón: empieza la tirada
    if pulsado:
        pulsado = False
        if not animando:
            random.seed(ticks_us())
            animando = True
            t_ini = ticks_ms()
            t_cambio = 0

    # 3) Animación y resultado (no bloquea: el lector sigue funcionando)
    if animando:
        ahora = ticks_ms()
        if ticks_diff(ahora, t_ini) >= 1500:
            animando = False
            d1 = random.randint(1, 6)
            d2 = random.randint(1, 6)
            valores[0] = d1
            valores[1] = d2
            enviar("DADOS,%d,%d" % (d1, d2))
        elif ticks_diff(ahora, t_cambio) >= 80:
            valores[0] = random.randint(1, 6)
            valores[1] = random.randint(1, 6)
            t_cambio = ahora

    # 4) Lector RFID
    (estado, tipo) = rfid.request(rfid.REQIDL)
    if estado == rfid.OK:
        (estado2, uid) = rfid.SelectTagSN()
        if estado2 == rfid.OK:
            uid_str = "".join("{:02X}".format(b) for b in uid)
            ahora = ticks_ms()
            if uid_str != ultimo_uid or ticks_diff(ahora, t_uid) > 2000:
                enviar("TARJETA," + uid_str)
            ultimo_uid = uid_str
            t_uid = ahora

    sleep_ms(20)