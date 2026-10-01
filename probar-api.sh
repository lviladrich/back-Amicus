#!/bin/bash


API="http://localhost:8080/api"
SUFIJO=$$                      # numero unico, para no chocar con datos previos
OK=0
FALLOS=0

verde()  { printf "\033[32m%s\033[0m" "$1"; }
rojo()   { printf "\033[31m%s\033[0m" "$1"; }
titulo() { printf "\n\033[1;36m%s\033[0m\n" "$1"; }

# Ejecuta una peticion y compara el codigo HTTP con el esperado.
# El sexto parametro es opcional: credenciales "mail:contrasena" para Basic Auth.
# El septimo es opcional: un token JWT, para mandar "Authorization: Bearer <token>".
probar() {
    local descripcion="$1" esperado="$2" metodo="$3" ruta="$4" cuerpo="$5" credenciales="$6" token="$7"
    local codigo
    local auth=()
    if [ -n "$credenciales" ]; then
        auth=(-u "$credenciales")
    elif [ -n "$token" ]; then
        auth=(-H "Authorization: Bearer $token")
    fi

    if [ -n "$cuerpo" ]; then
        codigo=$(curl -s -o /tmp/amicus_resp.json -w "%{http_code}" "${auth[@]}" \
                 -X "$metodo" "$API$ruta" -H 'Content-Type: application/json' -d "$cuerpo")
    else
        codigo=$(curl -s -o /tmp/amicus_resp.json -w "%{http_code}" "${auth[@]}" -X "$metodo" "$API$ruta")
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
TOKEN_CLI=$(campo token)
probar "Rechazar password incorrecta"         401 POST "/auth/login" \
  "{\"email\":\"cli$SUFIJO@mail.com\",\"password\":\"equivocada\"}"

probar "Login del profesional"                200 POST "/auth/login" \
  "{\"email\":\"pro$SUFIJO@mail.com\",\"password\":\"secreto123\"}"
TOKEN_PRO=$(campo token)

probar "Editar el perfil propio"              200 PUT "/auth/usuarios/$CLI" \
  "{\"nombre\":\"Lucia Editada\",\"apellido\":\"Viladrich Editada\",\"email\":\"cli$SUFIJO.editado@mail.com\"}" "" "$TOKEN_CLI"
echo "     -> username no cambio: $(campo username)"

# El token viejo de la clienta quedo con el mail anterior adentro: el filtro
# JWT busca el usuario por ese mail en cada pedido (ver FiltroJwt), asi que
# tras cambiar el mail hay que volver a loguearse para tener un token valido.
probar "Login con el mail nuevo"              200 POST "/auth/login" \
  "{\"email\":\"cli$SUFIJO.editado@mail.com\",\"password\":\"secreto123\"}"
TOKEN_CLI=$(campo token)

probar "Rechazar que otro edite el perfil"    403 PUT "/auth/usuarios/$PRO" \
  "{\"nombre\":\"Hackeado\",\"apellido\":\"Hackeado\",\"email\":\"hack$SUFIJO@mail.com\"}" "" "$TOKEN_CLI"
probar "Rechazar mail duplicado en la edicion" 400 PUT "/auth/usuarios/$PRO" \
  "{\"nombre\":\"Martin\",\"apellido\":\"Gomez\",\"email\":\"cli$SUFIJO.editado@mail.com\"}" "" "$TOKEN_PRO"
probar "Rechazar datos invalidos en la edicion" 400 PUT "/auth/usuarios/$CLI" \
  '{"nombre":"","apellido":"Viladrich","email":"no-es-mail"}' "" "$TOKEN_CLI"

# ------------------------------------------------------------
titulo "3. PUBLICAR SERVICIOS"
probar "Sin token no se puede publicar"       401 POST "/servicios" \
  "{\"titulo\":\"Intento\",\"descripcion\":\"Sin loguearse\",\"precio\":100,\"cuposDisponibles\":1,\"categoriaId\":1}"

probar "Publicar servicio con 3 cupos"        201 POST "/servicios" \
  "{\"titulo\":\"Instalacion de ventilador $SUFIJO\",\"descripcion\":\"Incluye soporte, cableado y prueba de funcionamiento.\",\"precio\":25000.00,\"cuposDisponibles\":3,\"categoriaId\":1,\"zonaIds\":[4,2],\"imagenes\":[\"https://ejemplo.com/a.jpg\",\"https://ejemplo.com/b.jpg\"]}" "" "$TOKEN_PRO"
SERV=$(campo id)
echo "     -> id del servicio: $SERV"

probar "Publicar servicio SIN cupos"          201 POST "/servicios" \
  "{\"titulo\":\"Cambio de tablero $SUFIJO\",\"descripcion\":\"Reemplazo completo con termicas.\",\"precio\":80000.00,\"cuposDisponibles\":0,\"categoriaId\":1,\"zonaIds\":[4]}" "" "$TOKEN_PRO"
SIN_CUPOS=$(campo id)

probar "Rechazar precio en cero"              400 POST "/servicios" \
  "{\"titulo\":\"Gratis\",\"descripcion\":\"Prueba\",\"precio\":0,\"cuposDisponibles\":1,\"categoriaId\":1}" "" "$TOKEN_PRO"

# ------------------------------------------------------------
titulo "4. CATALOGO"
probar "Catalogo completo (alfabetico)"       200 GET "/servicios"
probar "Filtrar por categoria"                200 GET "/servicios?categoriaId=1"
probar "Filtrar por zona"                     200 GET "/servicios?zonaId=4"
probar "Buscar por texto"                     200 GET "/servicios?q=ventilador"
probar "Solo con cupos disponibles"           200 GET "/servicios?conCupo=true"
probar "Catalogo paginado (page=0, size=1)"   200 GET "/servicios?page=0&size=1"
echo "     -> pagina $(campo pagina) de $(campo totalPaginas), $(campo totalElementos) servicio(s) en total"
probar "Detalle del servicio"                 200 GET "/servicios/$SERV"
echo "     -> categoria: $(campo categoria | head -c 60)"
probar "Servicio inexistente da 404"          404 GET "/servicios/999999"

# ------------------------------------------------------------
titulo "5. REGLAS DEL CARRITO"
probar "Sin token no se puede usar el carrito" 401 GET "/carrito"

probar "Rechazar contratar lo propio"         400 POST "/carrito/items" \
  "{\"servicioId\":$SERV,\"cantidad\":1}" "" "$TOKEN_PRO"
echo "     -> $(campo mensaje)"

probar "Rechazar servicio SIN CUPOS"          409 POST "/carrito/items" \
  "{\"servicioId\":$SIN_CUPOS,\"cantidad\":1}" "" "$TOKEN_CLI"
echo "     -> $(campo mensaje)"

probar "Rechazar mas cantidad que cupos"      400 POST "/carrito/items" \
  "{\"servicioId\":$SERV,\"cantidad\":99}" "" "$TOKEN_CLI"
echo "     -> $(campo mensaje)"

probar "Rechazar cantidad cero"               400 POST "/carrito/items" \
  "{\"servicioId\":$SERV,\"cantidad\":0}" "" "$TOKEN_CLI"

# ------------------------------------------------------------
titulo "6. CARRITO"
probar "Agregar 2 unidades"                   201 POST "/carrito/items" \
  "{\"servicioId\":$SERV,\"cantidad\":2}" "" "$TOKEN_CLI"
probar "Agregar 1 mas del mismo servicio"     201 POST "/carrito/items" \
  "{\"servicioId\":$SERV,\"cantidad\":1}" "" "$TOKEN_CLI"
probar "Ver el carrito"                       200 GET "/carrito" "" "" "$TOKEN_CLI"
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
probar "Confirmar la compra"                  201 POST "/carrito/checkout" "" "" "$TOKEN_CLI"
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
probar "El carrito quedo vacio"               200 GET "/carrito" "" "" "$TOKEN_CLI"
echo "     -> items: $(campo cantidadDeItems), total: $(campo total)"
probar "Rechazar checkout con carrito vacio"  400 POST "/carrito/checkout" "" "" "$TOKEN_CLI"

# ------------------------------------------------------------
titulo "8. EL PRECIO CONGELADO"
echo "  El profesional sube el precio de 25.000 a 40.000:"
probar "Modificar la publicacion"             200 PUT "/servicios/$SERV" \
  "{\"titulo\":\"Instalacion de ventilador $SUFIJO\",\"descripcion\":\"Descripcion actualizada.\",\"precio\":40000.00,\"cuposDisponibles\":5,\"categoriaId\":1,\"zonaIds\":[4]}" "" "$TOKEN_PRO"
echo "     -> precio actual del servicio: \$$(campo precio)"
probar "Consultar la orden ya pagada"         200 GET "/ordenes/$ORDEN" "" "" "$TOKEN_CLI"
python3 -c "
import json
d = json.load(open('/tmp/amicus_resp.json'))
for i in d['items']:
    print('     -> la orden sigue diciendo \$%s' % i['precioUnitario'])
print('     -> el comprobante NO se reescribio')
"

# ------------------------------------------------------------
titulo "9. VALIDACION DE PROPIETARIO"
probar "Rechazar que otro modifique"          403 PUT "/servicios/$SERV" \
  "{\"titulo\":\"Secuestrado\",\"descripcion\":\"Intento de modificacion ajena.\",\"precio\":1.00,\"cuposDisponibles\":99,\"categoriaId\":1,\"zonaIds\":[4]}" "" "$TOKEN_CLI"
echo "     -> $(campo mensaje)"
probar "Rechazar que otro cambie los cupos"   403 PATCH "/servicios/$SERV/cupos" '{"cuposDisponibles":99}' "" "$TOKEN_CLI"
probar "Rechazar que otro agregue fotos"      403 POST "/servicios/$SERV/imagenes" '{"url":"https://ejemplo.com/ajena.jpg"}' "" "$TOKEN_CLI"
probar "Rechazar que otro de de baja"         403 DELETE "/servicios/$SERV" "" "" "$TOKEN_CLI"
echo "     -> $(campo mensaje)"
probar "El dueno SI puede ajustar cupos"      200 PATCH "/servicios/$SERV/cupos" '{"cuposDisponibles":4}' "" "$TOKEN_PRO"

titulo "10. BAJA LOGICA"
probar "Ajustar cupos con PATCH"              200 PATCH "/servicios/$SIN_CUPOS/cupos" '{"cuposDisponibles":7}' "" "$TOKEN_PRO"
probar "Dar de baja el servicio"              204 DELETE "/servicios/$SERV" "" "" "$TOKEN_PRO"
probar "Rechazar la segunda baja"             400 DELETE "/servicios/$SERV" "" "" "$TOKEN_PRO"
probar "La orden sigue existiendo"            200 GET "/ordenes/$ORDEN" "" "" "$TOKEN_CLI"
echo "     -> total de la orden: \$$(campo total)"
probar "Historial de compras"                 200 GET "/ordenes" "" "" "$TOKEN_CLI"
probar "Historial paginado (page=0, size=1)"  200 GET "/ordenes?page=0&size=1" "" "" "$TOKEN_CLI"
echo "     -> pagina $(campo pagina) de $(campo totalPaginas), $(campo totalElementos) orden(es) en total"

# ------------------------------------------------------------
titulo "11. RECURRENCIA"
echo "  El servicio $SIN_CUPOS quedo con 7 cupos. Se contrata semanalmente:"
probar "Rechazar recurrencia de 1 sola visita"  400 POST "/carrito/items" \
  "{\"servicioId\":$SIN_CUPOS,\"cantidad\":1,\"frecuencia\":\"SEMANAL\"}" "" "$TOKEN_CLI"
echo "     -> $(campo mensaje)"

probar "Contratar 2 visitas SEMANAL"            201 POST "/carrito/items" \
  "{\"servicioId\":$SIN_CUPOS,\"cantidad\":2,\"frecuencia\":\"SEMANAL\"}" "" "$TOKEN_CLI"
python3 -c "
import json
d = json.load(open('/tmp/amicus_resp.json'))
for i in d['items']:
    print('     -> %s x%s %s = \$%s' % (i['titulo'], i['cantidad'], i['frecuencia'], i['subtotal']))
print('     -> la frecuencia no cambia el precio, solo cuando se presta')
"
# El id del item se lee del GET y no de la respuesta del POST: al agregar una
# linea nueva, el POST la devuelve con id null porque todavia no se hizo flush.
probar "Ver el carrito para tomar el id"        200 GET "/carrito" "" "" "$TOKEN_CLI"
ITEM_REC=$(python3 -c "import json;print(json.load(open('/tmp/amicus_resp.json'))['items'][0]['id'])")
probar "Cambiar la frecuencia a MENSUAL"        200 PUT "/carrito/items/$ITEM_REC" \
  '{"cantidad":2,"frecuencia":"MENSUAL"}' "" "$TOKEN_CLI"
echo "     -> frecuencia: $(python3 -c "import json;print(json.load(open('/tmp/amicus_resp.json'))['items'][0]['frecuencia'])")"

probar "Confirmar la contratacion recurrente"   201 POST "/carrito/checkout" "" "" "$TOKEN_CLI"
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
probar "Login del vecino"                       200 POST "/auth/login" \
  "{\"email\":\"vec$SUFIJO@mail.com\",\"password\":\"secreto123\"}"
TOKEN_VEC=$(campo token)

probar "Rechazar resenia del propio dueno"      400 POST "/servicios/$SERV/resenas" \
  '{"puntaje":5,"comentario":"Me califico a mi mismo"}' "" "$TOKEN_PRO"
echo "     -> $(campo mensaje)"

probar "Rechazar a quien no lo contrato"        403 POST "/servicios/$SERV/resenas" \
  '{"puntaje":1,"comentario":"Nunca lo use pero opino"}' "" "$TOKEN_VEC"
echo "     -> $(campo mensaje)"

probar "Rechazar puntaje fuera de 1 a 5"        400 POST "/servicios/$SERV/resenas" \
  '{"puntaje":9,"comentario":"Once de diez"}' "" "$TOKEN_CLI"

probar "La clienta que SI contrato resenia"     201 POST "/servicios/$SERV/resenas" \
  '{"puntaje":4,"comentario":"Llego puntual y dejo todo limpio."}' "" "$TOKEN_CLI"
RESENIA=$(campo id)
echo "     -> puntaje $(campo puntaje) de $(campo autor)"

probar "Rechazar la segunda resenia del mismo"  409 POST "/servicios/$SERV/resenas" \
  '{"puntaje":1,"comentario":"Me arrepenti"}' "" "$TOKEN_CLI"
echo "     -> $(campo mensaje)"

probar "Listar las resenias del servicio"       200 GET "/servicios/$SERV/resenas"
probar "Resenias paginadas (page=0, size=1)"    200 GET "/servicios/$SERV/resenas?page=0&size=1"
echo "     -> pagina $(campo pagina) de $(campo totalPaginas), $(campo totalElementos) resenia(s) en total"
probar "El detalle muestra la calificacion"     200 GET "/servicios/$SERV"
echo "     -> promedio: $(campo promedioPuntaje) sobre $(campo cantidadResenas) resenia(s)"

probar "Rechazar que otro borre la resenia"     403 DELETE "/resenas/$RESENIA" "" "" "$TOKEN_VEC"
probar "El autor SI puede borrar la suya"       204 DELETE "/resenas/$RESENIA" "" "" "$TOKEN_CLI"
probar "Sin resenias el promedio es nulo"       200 GET "/servicios/$SERV"
python3 -c "
import json
p = json.load(open('/tmp/amicus_resp.json'))['promedioPuntaje']
print('     -> promedio: %s' % ('null' if p is None else p))
print('     -> null y no 0: un servicio sin resenias no vale cero estrellas')
"

# ------------------------------------------------------------
titulo "13. CANCELAR UNA ORDEN"
probar "Rechazar que otro cancele"              403 PATCH "/ordenes/$ORDEN_REC/cancelar" "" "" "$TOKEN_VEC"
echo "     -> $(campo mensaje)"
probar "Cancelar una orden inexistente da 404"  404 PATCH "/ordenes/999999/cancelar" "" "" "$TOKEN_CLI"

probar "El dueno cancela su orden"              200 PATCH "/ordenes/$ORDEN_REC/cancelar" "" "" "$TOKEN_CLI"
echo "     -> estado: $(campo estado), total intacto: \$$(campo total)"
probar "Los cupos volvieron a 7"                200 GET "/servicios/$SIN_CUPOS"
echo "     -> cupos: $(campo cuposDisponibles)"
probar "Rechazar la segunda cancelacion"        400 PATCH "/ordenes/$ORDEN_REC/cancelar" "" "" "$TOKEN_CLI"
echo "     -> $(campo mensaje)"
probar "La orden cancelada sigue en el historial" 200 GET "/ordenes/$ORDEN_REC" "" "" "$TOKEN_CLI"
echo "     -> un comprobante cancelado sigue siendo un comprobante"

# ------------------------------------------------------------
titulo "14. PEDIDOS MAL FORMADOS"
probar "Metodo no permitido da 405"             405 DELETE "/auth/usuarios/1"
probar "JSON malformado da 400"                 400 POST "/auth/login" '{esto no es json}'
probar "Un id invalido en la ruta da 400"       400 GET "/servicios/-1"
probar "Tipo invalido en la ruta da 400"        400 GET "/servicios/abc"
probar "Ruta inexistente da 404"                404 GET "/no-existe"

# ------------------------------------------------------------
titulo "15. ABM COMPLETO DE CATEGORIAS Y ZONAS (solo ADMIN)"
ADMIN="admin@amicus.com:admin123"

probar "Sin credenciales da 401"                401 POST "/categorias" \
  "{\"nombre\":\"Intento$SUFIJO\",\"descripcion\":\"x\"}"
probar "Un usuario sin rol ADMIN da 403"        403 POST "/categorias" \
  "{\"nombre\":\"Intento$SUFIJO\",\"descripcion\":\"x\"}" "pro$SUFIJO@mail.com:secreto123"

probar "Crear categoria de prueba"              201 POST "/categorias" \
  "{\"nombre\":\"CategoriaPrueba$SUFIJO\",\"descripcion\":\"Solo para probar el ABM\"}" "$ADMIN"
CAT_PRUEBA=$(campo id)
probar "Buscar la categoria creada"             200 GET "/categorias/$CAT_PRUEBA"
probar "Categoria inexistente da 404"           404 GET "/categorias/999999"
probar "No se puede borrar una categoria en uso" 409 DELETE "/categorias/1" "" "$ADMIN"
echo "     -> $(campo mensaje)"
probar "Borrar la categoria de prueba"          204 DELETE "/categorias/$CAT_PRUEBA" "" "$ADMIN"
probar "La categoria borrada ya no existe"      404 GET "/categorias/$CAT_PRUEBA"

probar "Crear zona de prueba"                   201 POST "/zonas" \
  "{\"nombre\":\"ZonaPrueba$SUFIJO\"}" "$ADMIN"
ZONA_PRUEBA=$(campo id)
probar "Buscar la zona creada"                  200 GET "/zonas/$ZONA_PRUEBA"
probar "Zona inexistente da 404"                404 GET "/zonas/999999"
probar "Modificar el nombre de la zona"         200 PUT "/zonas/$ZONA_PRUEBA" \
  "{\"nombre\":\"ZonaPruebaModificada$SUFIJO\"}" "$ADMIN"
echo "     -> nombre: $(campo nombre)"
probar "No se puede borrar una zona en uso"     409 DELETE "/zonas/4" "" "$ADMIN"
echo "     -> $(campo mensaje)"
probar "Borrar la zona de prueba"               204 DELETE "/zonas/$ZONA_PRUEBA" "" "$ADMIN"
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
