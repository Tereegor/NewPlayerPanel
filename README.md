# NewPlayerPanel
## Возможности

- **Villager Tracker** - отслеживание убийств жителей
- **TNT Protection** - защита от использования TNT для новых игроков
- **Restrictions System** - система временных ограничений для игроков

## Команды

### Villager Tracker

- `/villagerhistory` - показать все записи
- `/villagerhistory <игрок>` - записи конкретного игрока
- `/villagerhistory <x> <y> <z>` - записи по координатам (±2 блока)
- `/villagerhistory coords` - вставляет текущие координаты

### Restrictions

- `/restrict <игрок> <ограничение> <время>` - применить ограничение
  - `0` - снять ограничение
  - `-1` - перманентно
  - `> 0` - время в секундах
- `/unrestrict <игрок> <ограничение|all>` - снять ограничение
- `/restrictions [игрок]` - просмотр активных ограничений

### TNT Protection

- `/tntprotection` или `/tnt` - справка
- `/tntprotection time [игрок]` - показать время игрока и доступность TNT
- `/tntprotection settings` - показать настройки защиты
- `/tntprotection reload` - перезагрузить конфигурацию

## Разрешения

- `newplayerpanel.history` - просмотр истории жителей (по умолчанию: op)
- `newplayerpanel.notify` - получение уведомлений о смерти жителей (по умолчанию: false)
- `newplayerpanel.restrictions.bypass` - обход всех ограничений (по умолчанию: op)
- `newplayerpanel.restrictions.restrict` - применение ограничений (по умолчанию: op)
- `newplayerpanel.restrictions.view` - просмотр ограничений (по умолчанию: op)
- `newplayerpanel.tntprotection.bypass` - обход TNT Protection (по умолчанию: op)
- `newplayerpanel.tntprotection.manage` - управление TNT Protection (по умолчанию: op)

## Конфигурация

### restrictions.yml

Настройка ограничений для игроков. Пример:

```yaml
restrictions:
  - name: villager_hit_restriction
    type: ENTITY
    actions: DAMAGE
    entity: [minecraft:villager]
    time: 0
    default: true
```

### tntprotection.yml

Настройка защиты от TNT. По умолчанию требуется 8 часов на сервере.

## Примеры использования

**Применить ограничение на 5 минут:**
```
/restrict Player villager_hit_restriction 300
```

**Применить перманентное ограничение:**
```
/restrict Player villager_hit_restriction -1
```

**Снять ограничение:**
```
/restrict Player villager_hit_restriction 0
```

**Разрешить игроку использовать TNT раньше времени:**
```
/restrict Player tnt_place_restriction -1
```

## Требования

- Minecraft Server: Purpur 1.21.10
- Java: 21

## Справка

- Автор - Math_Tereegor
- Плагин создан для проекта DuckHood
- Подали идею и помогли 6oJIeH и MISQZY
