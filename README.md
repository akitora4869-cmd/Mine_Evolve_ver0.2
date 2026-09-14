# Project EVOLVE v0.3.2

Paper 1.21.1 / Java 21 prototype.

## v0.3.2 changes

- Monster test body is visible to the Monster controller again.
- Experimental fixed third-person Monster camera using ProtocolLib.
- Camera follows behind/above the controlled Monster.
- Stage-specific camera distance/height.
- Camera ray traces blocks and moves closer when a wall is behind the Monster.
- Existing melee hitbox, attack effects, wildlife, corpses, feeding and Evolution remain enabled.
- Hunter uses the normal player camera. Minecraft's F5 perspective key is client-side, so a server-only plugin cannot truly prevent Hunters from pressing F5.

## Required plugins

1. Paper 1.21.1
2. ProtocolLib 5.4.0
3. ProjectEVOLVE v0.3.2

ProtocolLib is a **server plugin** and must be placed in `plugins/` next to ProjectEVOLVE. It is not bundled into the EVOLVE jar.

## Test

```text
/evolve monster
/evolve wildlife 6
```

After `/evolve monster`, the controller should see the enlarged Skeleton from a trailing TPS camera instead of from inside the model.

Use left click to attack. Attack feedback still displays `ATTACK! HIT xN` or `ATTACK! MISS` and shows slash/crit particles.

```text
/evolve levelup
```

Stage camera defaults:

- Stage 1 Skeleton (1.5x): distance 4.2, height 2.1
- Stage 2 Wither Skeleton (2.0x): distance 5.6, height 3.0
- Stage 3 Wither (1.6x): distance 8.0, height 4.2

They can be edited in `config.yml` under `monster.camera`.

## Important prototype note

The Monster TPS camera uses Minecraft's camera packet through ProtocolLib. This is an experiment for the future Model Engine Monster Controller. If a specific client/server build behaves differently, use `/evolve reset` or reconnect to restore the normal camera, and report the console/client behavior.
