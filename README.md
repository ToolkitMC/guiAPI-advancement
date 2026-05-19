# guiAPI-advancement

**A lightweight addon for [guiAPI](https://github.com/ToolkitMC/guiAPI?utm_source=chatgpt.com) that opens custom GUIs when players earn advancements.**

This mod allows you to create immersive experiences by automatically opening datapack-defined GUIs when an advancement is completed — useful for tutorials, story events, reward screens, introductions, and progression systems.

## Features

* **Datapack-driven configuration** — no coding required
* Open any guiAPI GUI when a player completes a specific advancement
* Configurable **delay** (in ticks) before the GUI opens
* **`once` mode** — display the GUI only the first time the advancement is earned
* Supports both vanilla and custom advancements
* Lightweight Fabric mixin-based implementation
* Supports `/reload`

## Dependencies

* **Minecraft** `1.21.1`
* **Fabric Loader** `0.16.5+`
* **Fabric API**
* **[guiAPI](https://github.com/ToolkitMC/guiAPI?utm_source=chatgpt.com)** *(required)*

## Installation

1. Download the latest release from [Releases](https://github.com/ToolkitMC/guiAPI-advancement/releases?utm_source=chatgpt.com)
2. Place the `.jar` file into your `mods/` folder
3. Install **guiAPI**
4. Launch the game

## Usage (Datapack Configuration)

Create JSON files in your datapack at:

```text
data/<namespace>/advancement/<file>.json
```

### Example

`data/mypack/advancement/welcome.json`

```json
{
  "advancement": "minecraft:story/obtain_armor",
  "gui": "mypack:welcome_screen",
  "delay_ticks": 20,
  "once": true
}
```

## Fields

| Field         | Type    | Default  | Description                                                            |
| ------------- | ------- | -------- | ---------------------------------------------------------------------- |
| `advancement` | String  | Required | Advancement ID that triggers the GUI                                   |
| `gui`         | String  | Required | GUI ID registered through guiAPI                                       |
| `delay_ticks` | Integer | `0`      | Delay before opening the GUI                                           |
| `once`        | Boolean | `false`  | If `true`, the GUI only opens the first time the advancement is earned |

## How `once` Mode Works

When `once` is enabled, the mod adds a scoreboard tag to the player in the following format:

```text
guiadv.<namespace>.<path_with_dots>
```

Example:

```text
guiadv.minecraft.story.obtain_armor
```

This prevents the GUI from appearing again if the advancement is re-earned later.

## Building from Source

```bash
./gradlew build
```

The compiled `.jar` file will be generated in:

```text
build/libs/
```

## License

This project is licensed under the MIT License. See `LICENSE` for details.

## Credits

Built as part of the [ToolkitMC ecosystem](https://github.com/ToolkitMC?utm_source=chatgpt.com).
