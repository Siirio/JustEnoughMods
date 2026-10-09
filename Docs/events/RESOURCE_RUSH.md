# Resource Rush

## Primary state

Resource Rush — временный server-hosted event instance с отдельной областью, участниками, временем жизни ресурсов и возвратом. Граница охватывает всю высоту мира и dimension-filtered. Визуальный perimeter всегда показывает принятую целиком область непрерывной завесой, соединёнными рельсами и регулярными вертикальными маркерами по поверхности, без повторного client-side отсечения дальних частей прямоугольника. Резервирование допускается только на сухой прочной поверхности не менее чем в 90% columns; natural water не удаляется.

Ресурсы и placements принадлежат instance, а cleanup удаляет только созданное этим instance. Event preparation ставится в очередь; scheduler активирует не более одного подготовленного события за цикл и переиспользует cached recipe analysis.

Resource Rush не создаёт locked participant roster: игроки всегда свободно входят, выходят и возвращаются. Пока внутри нет игроков, mobs пересекают границу свободно; при появлении хотя бы одного игрока общий `EVENT_LOCK` добавляет mob-only collision geometry с обеих сторон без сохранения предыдущей позиции, teleport-back или velocity reversal. Кнопка Teleport переносит внутрь безопасной точки области; visual perimeter остаётся full-height и совпадает с collision coordinates.

## Rejected

- Поиск и генерация нескольких арен в один scheduler tick отвергнуты из-за server-thread stalls.
- Очистка natural water отвергнута: плохая локация должна быть отклонена до reservation.
- Координатная проверка без dimension отвергнута: одинаковые Nether/Overworld coordinates не являются одной ареной.

