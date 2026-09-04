#!/bin/bash


API="http://localhost:8080/api"
SUFIJO=$$                      # numero unico, para no chocar con datos previos
OK=0
FALLOS=0

verde()  { printf "\033[32m%s\033[0m" "$1"; }
rojo()   { printf "\033[31m%s\033[0m" "$1"; }
titulo() { printf "\n\033[1;36m%s\033[0m\n" "$1"; }

# Ejecuta una peticion y compara el codigo HTTP con el esperado.
probar() {
    local descripcion="$1" esperado="$2" metodo="$3" ruta="$4" cuerpo="$5"
    local codigo

    if [ -n "$cuerpo" ]; then
        codigo=$(curl -s -o /tmp/amicus_resp.json -w "%{http_code}" \
                 -X "$metodo" "$API$ruta" -H 'Content-Type: application/json' -d "$cuerpo")
    else
        codigo=$(curl -s -o /tmp/amicus_resp.json -w "%{http_code}" -X "$metodo" "$API$ruta")
    fi

    if [ "$codigo" = "$esperado" ]; then
        printf "  %s %-52s HTTP %s\n" "$(verde OK)" "$descripcion" "$codigo"
        OK=$((OK + 1))
    else
        printf "  %s %-52s HTTP %s (esperaba %s)\n" "$(rojo FALLA)" "$descripcion" "$codigo" "$esperado"
        FALLOS=$((FALLOS + 1))
    fi
}

# Extrae un campo del ultimo JSON recibido.
campo() { python3 -c "import json;print(json.load(open('/tmp/amicus_resp.json')).get('$1',''))" 2>/dev/null; }

# Muestra el ultimo JSON de forma legible.
mostrar() { python3 -m json.tool /tmp/amicus_resp.json 2>/dev/null | head -${1:-20}; }

# ------------------------------------------------------------
echo "============================================================"
echo "  AMICUS - Prueba completa de la API"
echo "============================================================"

if ! curl -s -o /dev/null --max-time 3 "$API/categorias"; then
    rojo "La aplicacion no responde en http://localhost:8080"
    echo
    echo "Levantala primero con:  ./mvnw spring-boot:run"
    exit 1
fi

# ------------------------------------------------------------
titulo "1. CARGA INICIAL DE DATOS"
probar "Listar categorias"                    200 GET "/categorias"
echo "     -> $(python3 -c "import json;d=json.load(open('/tmp/amicus_resp.json'));print(len(d),'categorias')")"
probar "Listar zonas"                         200 GET "/zonas"
echo "     -> $(python3 -c "import json;d=json.load(open('/tmp/amicus_resp.json'));print(len(d),'zonas')")"

# ------------------------------------------------------------
titulo "2. REGISTRO Y LOGIN"
probar "Registrar al profesional"             201 POST "/auth/registro" \
  "{\"username\":\"pro$SUFIJO\",\"email\":\"pro$SUFIJO@mail.com\",\"password\":\"secreto123\",\"nombre\":\"Martin\",\"apellido\":\"Gomez\"}"
PRO=$(campo id)
echo "     -> id del profesional: $PRO"

probar "Registrar a la clienta"               201 POST "/auth/registro" \
  "{\"username\":\"cli$SUFIJO\",\"email\":\"cli$SUFIJO@mail.com\",\"password\":\"secreto123\",\"nombre\":\"Lucia\",\"apellido\":\"Viladrich\"}"
CLI=$(campo id)
echo "     -> id de la clienta:   $CLI"

if grep -q password /tmp/amicus_resp.json; then
    printf "  %s La respuesta expone la contrasena\n" "$(rojo FALLA)"; FALLOS=$((FALLOS+1))
else
    printf "  %s %-52s\n" "$(verde OK)" "La respuesta NO expone la contrasena"; OK=$((OK+1))
fi

probar "Rechazar mail repetido"               400 POST "/auth/registro" \
  "{\"username\":\"otro$SUFIJO\",\"email\":\"cli$SUFIJO@mail.com\",\"password\":\"secreto123\",\"nombre\":\"A\",\"apellido\":\"B\"}"
probar "Rechazar datos invalidos"             400 POST "/auth/registro" \
  '{"username":"ab","email":"no-es-mail","password":"123","nombre":"","apellido":""}'
echo "     -> $(python3 -c "import json;d=json.load(open('/tmp/amicus_resp.json'));print(list(d.get('errores',{}).keys()))")"
probar "Login correcto"                       200 POST "/auth/login" \
  "{\"email\":\"cli$SUFIJO@mail.com\",\"password\":\"secreto123\"}"
probar "Rechazar password incorrecta"         401 POST "/auth/login" \
  "{\"email\":\"cli$SUFIJO@mail.com\",\"password\":\"equivocada\"}"

# ------------------------------------------------------------
titulo "3. PUBLICAR SERVICIOS"
probar "Publicar servicio con 3 cupos"        201 POST "/servicios" \
  "{\"titulo\":\"Instalacion de ventilador $SUFIJO\",\"descripcion\":\"Incluye soporte, cableado y prueba de funcionamiento.\",\"precio\":25000.00,\"cuposDisponibles\":3,\"categoriaId\":1,\"profesionalId\":$PRO,\"zonaIds\":[4,2],\"imagenes\":[\"https://ejemplo.com/a.jpg\",\"https://ejemplo.com/b.jpg\"]}"
SERV=$(campo id)
echo "     -> id del servicio: $SERV"

probar "Publicar servicio SIN cupos"          201 POST "/servicios" \
  "{\"titulo\":\"Cambio de tablero $SUFIJO\",\"descripcion\":\"Reemplazo completo con termicas.\",\"precio\":80000.00,\"cuposDisponibles\":0,\"categoriaId\":1,\"profesionalId\":$PRO,\"zonaIds\":[4]}"
SIN_CUPOS=$(campo id)

probar "Rechazar precio en cero"              400 POST "/servicios" \
  "{\"titulo\":\"Gratis\",\"descripcion\":\"Prueba\",\"precio\":0,\"cuposDisponibles\":1,\"categoriaId\":1,\"profesionalId\":$PRO}"

# ------------------------------------------------------------
titulo "4. CATALOGO"
probar "Catalogo completo (alfabetico)"       200 GET "/servicios"
probar "Filtrar por categoria"                200 GET "/servicios?categoriaId=1"
probar "Filtrar por zona"                     200 GET "/servicios?zonaId=4"
probar "Buscar por texto"                     200 GET "/servicios?q=ventilador"
probar "Solo con cupos disponibles"           200 GET "/servicios?conCupo=true"
probar "Detalle del servicio"                 200 GET "/servicios/$SERV"
echo "     -> categoria: $(campo categoria | head -c 60)"
probar "Servicio inexistente da 404"          404 GET "/servicios/999999"

# ------------------------------------------------------------
titulo "5. REGLAS DEL CARRITO"
probar "Rechazar contratar lo propio"         400 POST "/carrito/items?usuarioId=$PRO" \
  "{\"servicioId\":$SERV,\"cantidad\":1}"
echo "     -> $(campo mensaje)"

probar "Rechazar servicio SIN CUPOS"          409 POST "/carrito/items?usuarioId=$CLI" \
  "{\"servicioId\":$SIN_CUPOS,\"cantidad\":1}"
echo "     -> $(campo mensaje)"

probar "Rechazar mas cantidad que cupos"      400 POST "/carrito/items?usuarioId=$CLI" \
  "{\"servicioId\":$SERV,\"cantidad\":99}"
echo "     -> $(campo mensaje)"

probar "Rechazar cantidad cero"               400 POST "/carrito/items?usuarioId=$CLI" \
  "{\"servicioId\":$SERV,\"cantidad\":0}"

# ------------------------------------------------------------
titulo "6. CARRITO"
probar "Agregar 2 unidades"                   201 POST "/carrito/items?usuarioId=$CLI" \
  "{\"servicioId\":$SERV,\"cantidad\":2}"
probar "Agregar 1 mas del mismo servicio"     201 POST "/carrito/items?usuarioId=$CLI" \
  "{\"servicioId\":$SERV,\"cantidad\":1}"
probar "Ver el carrito"                       200 GET "/carrito?usuarioId=$CLI"
python3 -c "
import json
d = json.load(open('/tmp/amicus_resp.json'))
for i in d['items']:
    print('     -> %s x%s = \$%s' % (i['titulo'], i['cantidad'], i['subtotal']))
print('     -> TOTAL: \$%s en %d linea(s)' % (d['total'], d['cantidadDeItems']))
print('     -> una sola linea con cantidad 3: no duplico la fila')
"

# ------------------------------------------------------------
titulo "7. CHECKOUT"
probar "Confirmar la compra"                  201 POST "/carrito/checkout?usuarioId=$CLI"
ORDEN=$(campo id)
python3 -c "
import json
d = json.load(open('/tmp/amicus_resp.json'))
print('     -> orden #%s, estado %s, total \$%s' % (d['id'], d['estado'], d['total']))
for i in d['items']:
    print('        %s x%s a \$%s' % (i['titulo'], i['cantidad'], i['precioUnitario']))
"
probar "El servicio quedo sin cupos"          200 GET "/servicios/$SERV"
echo "     -> cupos: $(campo cuposDisponibles), disponible: $(campo disponible)"
probar "El carrito quedo vacio"               200 GET "/carrito?usuarioId=$CLI"
echo "     -> items: $(campo cantidadDeItems), total: $(campo total)"
probar "Rechazar checkout con carrito vacio"  400 POST "/carrito/checkout?usuarioId=$CLI"

# ------------------------------------------------------------
titulo "8. EL PRECIO CONGELADO"
echo "  El profesional sube el precio de 25.000 a 40.000:"
probar "Modificar la publicacion"             200 PUT "/servicios/$SERV?usuarioId=$PRO" \
  "{\"titulo\":\"Instalacion de ventilador $SUFIJO\",\"descripcion\":\"Descripcion actualizada.\",\"precio\":40000.00,\"cuposDisponibles\":5,\"categoriaId\":1,\"profesionalId\":$PRO,\"zonaIds\":[4]}"
echo "     -> precio actual del servicio: \$$(campo precio)"
probar "Consultar la orden ya pagada"         200 GET "/ordenes/$ORDEN"
python3 -c "
import json
d = json.load(open('/tmp/amicus_resp.json'))
for i in d['items']:
    print('     -> la orden sigue diciendo \$%s' % i['precioUnitario'])
print('     -> el comprobante NO se reescribio')
"

# ------------------------------------------------------------
titulo "9. VALIDACION DE PROPIETARIO"
probar "Rechazar que otro modifique"          403 PUT "/servicios/$SERV?usuarioId=$CLI" \
  "{\"titulo\":\"Secuestrado\",\"descripcion\":\"Intento de modificacion ajena.\",\"precio\":1.00,\"cuposDisponibles\":99,\"categoriaId\":1,\"profesionalId\":$CLI,\"zonaIds\":[4]}"
echo "     -> $(campo mensaje)"
probar "Rechazar que otro cambie los cupos"   403 PATCH "/servicios/$SERV/cupos?usuarioId=$CLI" '{"cuposDisponibles":99}'
probar "Rechazar que otro agregue fotos"      403 POST "/servicios/$SERV/imagenes?usuarioId=$CLI" '{"url":"https://ejemplo.com/ajena.jpg"}'
probar "Rechazar que otro de de baja"         403 DELETE "/servicios/$SERV?usuarioId=$CLI"
echo "     -> $(campo mensaje)"
probar "El dueno SI puede ajustar cupos"      200 PATCH "/servicios/$SERV/cupos?usuarioId=$PRO" '{"cuposDisponibles":4}'

titulo "10. BAJA LOGICA"
probar "Ajustar cupos con PATCH"              200 PATCH "/servicios/$SIN_CUPOS/cupos?usuarioId=$PRO" '{"cuposDisponibles":7}'
probar "Dar de baja el servicio"              204 DELETE "/servicios/$SERV?usuarioId=$PRO"
probar "Rechazar la segunda baja"             400 DELETE "/servicios/$SERV?usuarioId=$PRO"
probar "La orden sigue existiendo"            200 GET "/ordenes/$ORDEN"
echo "     -> total de la orden: \$$(campo total)"
probar "Historial de compras"                 200 GET "/ordenes?usuarioId=$CLI"

# ------------------------------------------------------------
titulo "11. RECURRENCIA"
echo "  El servicio $SIN_CUPOS quedo con 7 cupos. Se contrata semanalmente:"
probar "Rechazar recurrencia de 1 sola visita"  400 POST "/carrito/items?usuarioId=$CLI" \
  "{\"servicioId\":$SIN_CUPOS,\"cantidad\":1,\"frecuencia\":\"SEMANAL\"}"
echo "     -> $(campo mensaje)"

probar "Contratar 2 visitas SEMANAL"            201 POST "/carrito/items?usuarioId=$CLI" \
  "{\"servicioId\":$SIN_CUPOS,\"cantidad\":2,\"frecuencia\":\"SEMANAL\"}"
python3 -c "
import json
d = json.load(open('/tmp/amicus_resp.json'))
for i in d['items']:
    print('     -> %s x%s %s = \$%s' % (i['titulo'], i['cantidad'], i['frecuencia'], i['subtotal']))
print('     -> la frecuencia no cambia el precio, solo cuando se presta')
"
# El id del item se lee del GET y no de la respuesta del POST: al agregar una
# linea nueva, el POST la devuelve con id null porque todavia no se hizo flush.
probar "Ver el carrito para tomar el id"        200 GET "/carrito?usuarioId=$CLI"
ITEM_REC=$(python3 -c "import json;print(json.load(open('/tmp/amicus_resp.json'))['items'][0]['id'])")
probar "Cambiar la frecuencia a MENSUAL"        200 PUT "/carrito/items/$ITEM_REC?usuarioId=$CLI" \
  '{"cantidad":2,"frecuencia":"MENSUAL"}'
echo "     -> frecuencia: $(python3 -c "import json;print(json.load(open('/tmp/amicus_resp.json'))['items'][0]['frecuencia'])")"

probar "Confirmar la contratacion recurrente"   201 POST "/carrito/checkout?usuarioId=$CLI"
ORDEN_REC=$(campo id)
python3 -c "
import json
d = json.load(open('/tmp/amicus_resp.json'))
for i in d['items']:
    print('     -> la orden congelo la frecuencia: %s' % i['frecuencia'])
"
probar "Se descontaron 2 de los 7 cupos"        200 GET "/servicios/$SIN_CUPOS"
echo "     -> cupos: $(campo cuposDisponibles)"

# ------------------------------------------------------------
titulo "12. RESENIAS"
probar "Registrar a un vecino que no contrato"  201 POST "/auth/registro" \
  "{\"username\":\"vec$SUFIJO\",\"email\":\"vec$SUFIJO@mail.com\",\"password\":\"secreto123\",\"nombre\":\"Vecino\",\"apellido\":\"Curioso\"}"
VEC=$(campo id)

probar "Rechazar resenia del propio dueno"      400 POST "/servicios/$SERV/resenas?usuarioId=$PRO" \
  '{"puntaje":5,"comentario":"Me califico a mi mismo"}'
echo "     -> $(campo mensaje)"

probar "Rechazar a quien no lo contrato"        403 POST "/servicios/$SERV/resenas?usuarioId=$VEC" \
  '{"puntaje":1,"comentario":"Nunca lo use pero opino"}'
echo "     -> $(campo mensaje)"

probar "Rechazar puntaje fuera de 1 a 5"        400 POST "/servicios/$SERV/resenas?usuarioId=$CLI" \
  '{"puntaje":9,"comentario":"Once de diez"}'

probar "La clienta que SI contrato resenia"     201 POST "/servicios/$SERV/resenas?usuarioId=$CLI" \
  '{"puntaje":4,"comentario":"Llego puntual y dejo todo limpio."}'
RESENIA=$(campo id)
echo "     -> puntaje $(campo puntaje) de $(campo autor)"

probar "Rechazar la segunda resenia del mismo"  409 POST "/servicios/$SERV/resenas?usuarioId=$CLI" \
  '{"puntaje":1,"comentario":"Me arrepenti"}'
echo "     -> $(campo mensaje)"

probar "Listar las resenias del servicio"       200 GET "/servicios/$SERV/resenas"
probar "El detalle muestra la calificacion"     200 GET "/servicios/$SERV"
echo "     -> promedio: $(campo promedioPuntaje) sobre $(campo cantidadResenas) resenia(s)"

probar "Rechazar que otro borre la resenia"     403 DELETE "/resenas/$RESENIA?usuarioId=$VEC"
probar "El autor SI puede borrar la suya"       204 DELETE "/resenas/$RESENIA?usuarioId=$CLI"
probar "Sin resenias el promedio es nulo"       200 GET "/servicios/$SERV"
python3 -c "
import json
p = json.load(open('/tmp/amicus_resp.json'))['promedioPuntaje']
print('     -> promedio: %s' % ('null' if p is None else p))
print('     -> null y no 0: un servicio sin resenias no vale cero estrellas')
"

# ------------------------------------------------------------
titulo "13. CANCELAR UNA ORDEN"
probar "Rechazar que otro cancele"              403 PATCH "/ordenes/$ORDEN_REC/cancelar?usuarioId=$VEC"
echo "     -> $(campo mensaje)"
probar "Cancelar una orden inexistente da 404"  404 PATCH "/ordenes/999999/cancelar?usuarioId=$CLI"

probar "El dueno cancela su orden"              200 PATCH "/ordenes/$ORDEN_REC/cancelar?usuarioId=$CLI"
echo "     -> estado: $(campo estado), total intacto: \$$(campo total)"
probar "Los cupos volvieron a 7"                200 GET "/servicios/$SIN_CUPOS"
echo "     -> cupos: $(campo cuposDisponibles)"
probar "Rechazar la segunda cancelacion"        400 PATCH "/ordenes/$ORDEN_REC/cancelar?usuarioId=$CLI"
echo "     -> $(campo mensaje)"
probar "La orden cancelada sigue en el historial" 200 GET "/ordenes/$ORDEN_REC"
echo "     -> un comprobante cancelado sigue siendo un comprobante"

# ------------------------------------------------------------
titulo "14. PEDIDOS MAL FORMADOS"
probar "Metodo no permitido da 405"             405 DELETE "/auth/usuarios/1"
probar "JSON malformado da 400"                 400 POST "/auth/login" '{esto no es json}'
probar "Falta parametro obligatorio da 400"     400 GET "/carrito"
probar "Tipo invalido en la ruta da 400"        400 GET "/servicios/abc"
probar "Ruta inexistente da 404"                404 GET "/no-existe"

# ------------------------------------------------------------
titulo "15. ABM COMPLETO DE CATEGORIAS Y ZONAS"
probar "Crear categoria de prueba"              201 POST "/categorias" \
  "{\"nombre\":\"CategoriaPrueba$SUFIJO\",\"descripcion\":\"Solo para probar el ABM\"}"
CAT_PRUEBA=$(campo id)
probar "Buscar la categoria creada"             200 GET "/categorias/$CAT_PRUEBA"
probar "Categoria inexistente da 404"           404 GET "/categorias/999999"
probar "No se puede borrar una categoria en uso" 409 DELETE "/categorias/1"
echo "     -> $(campo mensaje)"
probar "Borrar la categoria de prueba"          204 DELETE "/categorias/$CAT_PRUEBA"
probar "La categoria borrada ya no existe"      404 GET "/categorias/$CAT_PRUEBA"

probar "Crear zona de prueba"                   201 POST "/zonas" \
  "{\"nombre\":\"ZonaPrueba$SUFIJO\"}"
ZONA_PRUEBA=$(campo id)
probar "Buscar la zona creada"                  200 GET "/zonas/$ZONA_PRUEBA"
probar "Zona inexistente da 404"                404 GET "/zonas/999999"
probar "Modificar el nombre de la zona"         200 PUT "/zonas/$ZONA_PRUEBA" \
  "{\"nombre\":\"ZonaPruebaModificada$SUFIJO\"}"
echo "     -> nombre: $(campo nombre)"
probar "No se puede borrar una zona en uso"     409 DELETE "/zonas/4"
echo "     -> $(campo mensaje)"
probar "Borrar la zona de prueba"               204 DELETE "/zonas/$ZONA_PRUEBA"
probar "La zona borrada ya no existe"           404 GET "/zonas/$ZONA_PRUEBA"

# ------------------------------------------------------------
echo
echo "============================================================"
if [ $FALLOS -eq 0 ]; then
    printf "  %s   %d pruebas pasaron, 0 fallaron\n" "$(verde 'TODO OK')" "$OK"
else
    printf "  %s   %d pasaron, %d fallaron\n" "$(rojo 'HAY FALLOS')" "$OK" "$FALLOS"
fi
echo "============================================================"
exit $FALLOS
