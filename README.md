# NewPlayerPanel v2.0.1

Плагин для администрирования игроков на Minecraft сервере.

## Возможности

- **Villager Tracker** — отслеживание убийств жителей с открытыми торгами
- **Restrictions System** — гибкая система временных ограничений для игроков
- **Удобное хранение данных** — SQLite база данных или JSON файлы
- **Мультиязычность** — поддержка русского и английского языков

## Команды

### История жителей

| Команда | Описание |
|---------|----------|
| `/history` | Показать все записи |
| `/history <игрок>` | Записи конкретного игрока |
| `/history <x> <y> <z>` | Записи по координатам (±2 блока) |
| `/history coords` | Записи по текущим координатам |
| `/history <игрок> <x> <y> <z>` | Комбинированный поиск |
| `/history purge <время>` | Очистить старые записи (7d, 30d, 1h...) |

### Ограничения

| Команда | Описание |
|---------|----------|
| `/restrict <игрок> <ограничение> <время>` | Применить ограничение |
| `/unrestrict <игрок> <ограничение\|all>` | Снять ограничение |
| `/restrictions [игрок]` | Просмотр активных ограничений |

**Форматы времени:**
- `0` — снять ограничение
- `-1` — перманентно
- `300` — 300 секунд
- `1h`, `7d`, `30d` — 1 час, 7 дней, 30 дней

## Разрешения

| Разрешение | Описание | По умолчанию |
|------------|----------|--------------|
| `newplayerpanel.history` | Просмотр истории жителей | op |
| `newplayerpanel.history.purge` | Очистка истории жителей | op |
| `newplayerpanel.notify` | Уведомления о смерти жителей | op |
| `newplayerpanel.restrictions.bypass` | Обход всех ограничений | op |
| `newplayerpanel.restrictions.restrict` | Применение ограничений | op |
| `newplayerpanel.restrictions.view` | Просмотр ограничений | op |

## Конфигурация

### config.yml

```yaml
language: ru
storage: database
villager-tracker:
  only-traded: true
  notify-enabled: true
```

### restrictions.yml

```yaml
restrictions:
  - name: villager_hit_restriction
    type: ENTITY
    actions: DAMAGE
    entity: [minecraft:villager]
    time: -1
    default: false
  
  - name: tnt_place_restriction
    type: ITEM
    actions: USE
    item: [minecraft:tnt]
    time: 28800
    default: true
```

**Типы ограничений:**
- `ENTITY` — действия с сущностями (DAMAGE)
- `ITEM` — действия с предметами (USE, DROP, EQUIP)
- `COMMAND` — команды (EXECUTE)

**Параметр default:**
- `true` — применяется ко всем игрокам
- `false` — применяется только через команду `/restrict`

## Хранилище данных

При `storage: database`:
- Все данные в `plugins/NewPlayerPanel/database.db`

При `storage: json`:
- `plugins/NewPlayerPanel/data/messages.json`
- `plugins/NewPlayerPanel/data/villager_deaths.json`
- `plugins/NewPlayerPanel/data/restrictions.json`

## Примеры использования

```bash
# Ограничить игрока на 5 минут
/restrict Player villager_hit_restriction 300
/restrict Player villager_hit_restriction 5m

# Перманентное ограничение
/restrict Player tnt_place_restriction -1

# Снять ограничение
/unrestrict Player tnt_place_restriction

# Снять все ограничения
/unrestrict Player all

# Очистить историю старше 7 дней
/history purge 7d
```

## Требования

- **Сервер:** Purpur/Paper/Bukkit 1.21+
- **Java:** 21

## Справка

- **Разработчик:** Math_Tereegor
- **Проект:** DuckHood
- **Идеи и помощь:** 6oJIeH, MISQZY
