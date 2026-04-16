'use client';

import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import * as THREE from 'three';
import { BLOCK, HOTBAR_ORDER, INITIAL_INVENTORY, blockLabel, createBlockTexturePatterns, getDropForBlock } from '../lib/blockTypes';
import { RECIPES, applyRecipe, canCraft } from '../lib/recipes';
import { collidesWorld, getBaseBlock, getBlock, isExposed, keyOf, raycastVoxel, terrainHeight } from '../lib/world';

const PLAYER_SIZE = { radius: 0.32, height: 1.8, eyeHeight: 1.62 };
const GRAVITY = 26;
const MOVE_SPEED = 5.2;
const JUMP_SPEED = 9.2;
const REACH = 6;
const RENDER_RADIUS = 10;
const MAX_BASE_RENDER_HEIGHT = 22;
const MAX_HEALTH = 10;
const DAY_SPEED = 0.012;
const HUD_SAMPLE_SECONDS = 0.15;

function hearts(health) {
  return Array.from({ length: MAX_HEALTH }, (_, index) => (index < health ? '♥' : '♡')).join(' ');
}

function hexToRgb(hex) {
  const normalized = hex.replace('#', '');
  const value = normalized.length === 3
    ? normalized.split('').map((char) => char + char).join('')
    : normalized;
  const int = Number.parseInt(value, 16);
  return {
    r: (int >> 16) & 255,
    g: (int >> 8) & 255,
    b: int & 255,
  };
}

function createPixelCanvasTexture(palette) {
  const size = 16;
  const canvas = document.createElement('canvas');
  canvas.width = size;
  canvas.height = size;
  const ctx = canvas.getContext('2d', { alpha: true });

  for (let y = 0; y < size; y += 1) {
    for (let x = 0; x < size; x += 1) {
      const i = (x * 3 + y * 5 + ((x ^ y) % 4)) % palette.length;
      ctx.fillStyle = palette[i];
      ctx.fillRect(x, y, 1, 1);
    }
  }

  const texture = new THREE.CanvasTexture(canvas);
  texture.colorSpace = THREE.SRGBColorSpace;
  texture.magFilter = THREE.NearestFilter;
  texture.minFilter = THREE.NearestFilter;
  texture.wrapS = THREE.ClampToEdgeWrapping;
  texture.wrapT = THREE.ClampToEdgeWrapping;
  texture.needsUpdate = true;
  return texture;
}

function makeBlockMaterialMap() {
  const patterns = createBlockTexturePatterns();
  const entries = {};

  Object.entries(patterns).forEach(([id, pattern]) => {
    const palette = pattern.all ?? pattern.side ?? pattern.top ?? ['#888888'];
    const transparent = Boolean(pattern.transparent);
    entries[id] = new THREE.MeshLambertMaterial({
      map: createPixelCanvasTexture(palette),
      transparent,
      alphaTest: transparent ? 0.35 : 0,
    });
  });

  return entries;
}

function getVisibleBlockPositions(edits, anchorX, anchorZ) {
  const groups = new Map();
  HOTBAR_ORDER.concat(BLOCK.LEAVES).forEach((id) => groups.set(id, []));

  for (let x = anchorX - RENDER_RADIUS; x <= anchorX + RENDER_RADIUS; x += 1) {
    for (let z = anchorZ - RENDER_RADIUS; z <= anchorZ + RENDER_RADIUS; z += 1) {
      const height = terrainHeight(x, z);
      const columnMaxY = Math.min(MAX_BASE_RENDER_HEIGHT, height + 6);
      for (let y = 0; y <= columnMaxY; y += 1) {
        const blockId = getBlock(x, y, z, edits);
        if (blockId !== BLOCK.AIR && isExposed(x, y, z, edits)) {
          groups.get(blockId)?.push([x, y, z]);
        }
      }
    }
  }

  edits.forEach((value, key) => {
    if (value === BLOCK.AIR) {
      return;
    }
    const [x, y, z] = key.split(',').map(Number);
    if (Math.abs(x - anchorX) > RENDER_RADIUS || Math.abs(z - anchorZ) > RENDER_RADIUS) {
      return;
    }
    if (isExposed(x, y, z, edits)) {
      groups.get(value)?.push([x, y, z]);
    }
  });

  return groups;
}

export default function MinecraftGame() {
  const mountRef = useRef(null);
  const engineRef = useRef(null);
  const unmountedRef = useRef(false);
  const inventoryRef = useRef({ ...INITIAL_INVENTORY });
  const selectedSlotRef = useRef(0);
  const targetBlockRef = useRef(null);
  const messageTimerRef = useRef(null);
  const healthRef = useRef(MAX_HEALTH);
  const anchorRef = useRef({ x: 0, z: 0 });
  const targetHitRef = useRef(null);

  const [inventory, setInventory] = useState({ ...INITIAL_INVENTORY });
  const [selectedSlot, setSelectedSlot] = useState(0);
  const [showCrafting, setShowCrafting] = useState(true);
  const [message, setMessage] = useState('Click Play to lock the cursor and start mining.');
  const [isLocked, setIsLocked] = useState(false);
  const [health, setHealth] = useState(MAX_HEALTH);
  const [playerSample, setPlayerSample] = useState({ x: 0, y: terrainHeight(0, 0) + 1.25, z: 0, onGround: false });
  const [timeOfDay, setTimeOfDay] = useState(0.25);
  const [targetState, setTargetState] = useState(null);

  const selectedBlock = HOTBAR_ORDER[selectedSlot];

  const setToast = useCallback((text) => {
    setMessage(text);
    if (messageTimerRef.current) {
      window.clearTimeout(messageTimerRef.current);
    }
    messageTimerRef.current = window.setTimeout(() => {
      if (!unmountedRef.current) {
        setMessage('');
      }
    }, 2400);
  }, []);

  useEffect(() => {
    inventoryRef.current = inventory;
  }, [inventory]);

  useEffect(() => {
    selectedSlotRef.current = selectedSlot;
  }, [selectedSlot]);

  const syncInventory = useCallback((updater) => {
    setInventory((current) => {
      const next = typeof updater === 'function' ? updater(current) : updater;
      inventoryRef.current = next;
      return next;
    });
  }, []);

  const rebuildWorld = useCallback(() => {
    const engine = engineRef.current;
    if (!engine) {
      return;
    }

    const { scene, worldGroup, materials, boxGeometry, tempObject, edits, instancedMeshes, player, selection } = engine;
    const nextAnchor = {
      x: Math.round(player.position.x),
      z: Math.round(player.position.z),
    };
    anchorRef.current = nextAnchor;

    const groups = getVisibleBlockPositions(edits, nextAnchor.x, nextAnchor.z);

    HOTBAR_ORDER.concat(BLOCK.LEAVES).forEach((blockId) => {
      const prev = instancedMeshes.get(blockId);
      if (prev) {
        worldGroup.remove(prev);
        prev.geometry?.dispose?.();
      }

      const positions = groups.get(blockId) ?? [];
      if (!positions.length) {
        instancedMeshes.delete(blockId);
        return;
      }

      const mesh = new THREE.InstancedMesh(boxGeometry, materials[blockId], positions.length);
      mesh.frustumCulled = false;
      positions.forEach((pos, index) => {
        tempObject.position.set(pos[0] + 0.5, pos[1] + 0.5, pos[2] + 0.5);
        tempObject.updateMatrix();
        mesh.setMatrixAt(index, tempObject.matrix);
      });
      mesh.instanceMatrix.needsUpdate = true;
      instancedMeshes.set(blockId, mesh);
      worldGroup.add(mesh);
    });

    scene.add(selection);
  }, []);

  const setWorldBlock = useCallback((x, y, z, nextBlock) => {
    const engine = engineRef.current;
    if (!engine) {
      return;
    }

    const key = keyOf(x, y, z);
    const base = getBaseBlock(x, y, z);
    if (nextBlock === base) {
      engine.edits.delete(key);
    } else {
      engine.edits.set(key, nextBlock);
    }
    rebuildWorld();
  }, [rebuildWorld]);

  const damageMobInFront = useCallback(() => {
    const engine = engineRef.current;
    if (!engine) {
      return false;
    }

    const origin = new THREE.Vector3(
      engine.player.position.x,
      engine.player.position.y + PLAYER_SIZE.eyeHeight,
      engine.player.position.z,
    );

    const direction = new THREE.Vector3(
      Math.sin(engine.controls.yaw) * Math.cos(engine.controls.pitch),
      Math.sin(engine.controls.pitch),
      -Math.cos(engine.controls.yaw) * Math.cos(engine.controls.pitch),
    ).normalize();

    let bestMob = null;
    let bestScore = Infinity;

    engine.mobs.forEach((mob) => {
      const toMob = new THREE.Vector3(mob.position.x, mob.position.y + 0.25, mob.position.z).sub(origin);
      const distance = toMob.length();
      if (distance > 4.2) {
        return;
      }
      toMob.normalize();
      const alignment = direction.dot(toMob);
      if (alignment < 0.92) {
        return;
      }
      const score = distance - alignment;
      if (score < bestScore) {
        bestScore = score;
        bestMob = mob;
      }
    });

    if (!bestMob) {
      return false;
    }

    bestMob.hp -= 1;
    if (bestMob.hp <= 0) {
      engine.scene.remove(bestMob.mesh);
      engine.mobs = engine.mobs.filter((mob) => mob.id !== bestMob.id);
      syncInventory((current) => ({ ...current, [BLOCK.STONE]: (current[BLOCK.STONE] ?? 0) + 1 }));
      setToast('Mob defeated. +1 stone');
    } else {
      setToast('Hit!');
    }
    return true;
  }, [setToast, syncInventory]);

  const breakTargetBlock = useCallback(() => {
    if (damageMobInFront()) {
      return;
    }

    const engine = engineRef.current;
    const target = targetHitRef.current?.block;
    if (!engine || !target) {
      return;
    }

    const blockId = getBlock(target.x, target.y, target.z, engine.edits);
    if (blockId === BLOCK.AIR) {
      return;
    }

    setWorldBlock(target.x, target.y, target.z, BLOCK.AIR);
    const drop = getDropForBlock(blockId);
    if (drop !== BLOCK.AIR) {
      syncInventory((current) => ({ ...current, [drop]: (current[drop] ?? 0) + 1 }));
      setToast(`Collected ${blockLabel(drop)}.`);
    }
  }, [damageMobInFront, setToast, setWorldBlock, syncInventory]);

  const placeTargetBlock = useCallback(() => {
    const engine = engineRef.current;
    const hit = targetHitRef.current;
    if (!engine || !hit) {
      return;
    }

    const activeBlock = HOTBAR_ORDER[selectedSlotRef.current];
    const inventoryCount = inventoryRef.current[activeBlock] ?? 0;
    if (inventoryCount <= 0) {
      return;
    }

    const placeX = hit.block.x + hit.faceNormal.x;
    const placeY = hit.block.y + hit.faceNormal.y;
    const placeZ = hit.block.z + hit.faceNormal.z;

    if (getBlock(placeX, placeY, placeZ, engine.edits) !== BLOCK.AIR) {
      return;
    }

    const playerPos = engine.player.position;
    const minX = playerPos.x - PLAYER_SIZE.radius;
    const maxX = playerPos.x + PLAYER_SIZE.radius;
    const minY = playerPos.y;
    const maxY = playerPos.y + PLAYER_SIZE.height;
    const minZ = playerPos.z - PLAYER_SIZE.radius;
    const maxZ = playerPos.z + PLAYER_SIZE.radius;
    const intersects = !(maxX <= placeX || minX >= placeX + 1 || maxY <= placeY || minY >= placeY + 1 || maxZ <= placeZ || minZ >= placeZ + 1);
    if (intersects) {
      setToast('Cannot place a block inside the player.');
      return;
    }

    setWorldBlock(placeX, placeY, placeZ, activeBlock);
    syncInventory((current) => ({ ...current, [activeBlock]: Math.max(0, (current[activeBlock] ?? 0) - 1) }));
    setToast(`Placed ${blockLabel(activeBlock)}.`);
  }, [setToast, setWorldBlock, syncInventory]);

  const craftRecipe = useCallback((recipe) => {
    if (!canCraft(recipe, inventoryRef.current)) {
      return;
    }
    syncInventory((current) => applyRecipe(recipe, current));
    setToast(`Crafted ${recipe.name}.`);
  }, [setToast, syncInventory]);

  const requestPlay = useCallback(() => {
    const engine = engineRef.current;
    if (!engine) {
      return;
    }
    const target = engine.renderer.domElement;
    if (target?.requestPointerLock) {
      target.requestPointerLock();
    }
    setToast('Cursor locked. Explore, mine, craft, and build.');
  }, [setToast]);

  useEffect(() => {
    const mount = mountRef.current;
    if (!mount) {
      return undefined;
    }

    unmountedRef.current = false;

    const renderer = new THREE.WebGLRenderer({ antialias: false, alpha: false, powerPreference: 'high-performance' });
    renderer.setPixelRatio(1);
    renderer.setSize(mount.clientWidth, mount.clientHeight);
    renderer.outputColorSpace = THREE.SRGBColorSpace;
    renderer.shadowMap.enabled = false;
    renderer.domElement.style.width = '100%';
    renderer.domElement.style.height = '100%';
    renderer.domElement.style.display = 'block';
    mount.appendChild(renderer.domElement);

    const scene = new THREE.Scene();
    const background = new THREE.Color('#7eb6ff');
    scene.background = background;
    scene.fog = new THREE.Fog('#7eb6ff', 24, 70);

    const camera = new THREE.PerspectiveCamera(75, mount.clientWidth / mount.clientHeight, 0.1, 120);
    camera.rotation.order = 'YXZ';

    const ambient = new THREE.AmbientLight('#ffffff', 0.88);
    scene.add(ambient);

    const sun = new THREE.DirectionalLight('#ffffff', 1.15);
    sun.position.set(18, 28, 12);
    scene.add(sun);

    const worldGroup = new THREE.Group();
    scene.add(worldGroup);

    const groundPlane = new THREE.Mesh(
      new THREE.PlaneGeometry(600, 600),
      new THREE.MeshLambertMaterial({ color: '#4b8a3f' }),
    );
    groundPlane.rotation.x = -Math.PI / 2;
    groundPlane.position.y = -0.001;
    scene.add(groundPlane);

    const boxGeometry = new THREE.BoxGeometry(1, 1, 1);
    const selection = new THREE.LineSegments(
      new THREE.EdgesGeometry(new THREE.BoxGeometry(1.03, 1.03, 1.03)),
      new THREE.LineBasicMaterial({ color: '#ffffff' }),
    );
    selection.visible = false;
    scene.add(selection);

    const materials = makeBlockMaterialMap();
    const tempObject = new THREE.Object3D();
    const playerStartY = terrainHeight(0, 0) + 1.25;

    engineRef.current = {
      renderer,
      scene,
      camera,
      ambient,
      sun,
      background,
      worldGroup,
      selection,
      boxGeometry,
      tempObject,
      materials,
      instancedMeshes: new Map(),
      edits: new Map(),
      controls: { yaw: 0, pitch: -0.35 },
      player: { position: new THREE.Vector3(0, playerStartY, 0), onGround: false },
      velocity: new THREE.Vector3(),
      forward: new THREE.Vector3(),
      right: new THREE.Vector3(),
      horizontal: new THREE.Vector3(),
      up: new THREE.Vector3(0, 1, 0),
      aimDirection: new THREE.Vector3(),
      lastFrameTime: performance.now(),
      timeOfDay: 0.25,
      hudAccumulator: 0,
      mobSpawnAccumulator: 0,
      mobs: [],
      animationFrameId: 0,
      pointerLocked: false,
      disposed: false,
    };

    const engine = engineRef.current;
    camera.position.set(0, playerStartY + PLAYER_SIZE.eyeHeight, 0);
    camera.rotation.y = 0;
    camera.rotation.x = -0.35;
    rebuildWorld();

    const resize = () => {
      if (!engineRef.current || engineRef.current.disposed) {
        return;
      }
      const width = mount.clientWidth || window.innerWidth;
      const height = mount.clientHeight || window.innerHeight;
      engine.camera.aspect = width / height;
      engine.camera.updateProjectionMatrix();
      engine.renderer.setSize(width, height);
    };

    const spawnMob = () => {
      const angle = Math.random() * Math.PI * 2;
      const distance = 8 + Math.random() * 8;
      const x = Math.round(engine.player.position.x + Math.cos(angle) * distance);
      const z = Math.round(engine.player.position.z + Math.sin(angle) * distance);
      const y = terrainHeight(x, z) + 1;

      const mob = {
        id: `mob-${Math.random().toString(36).slice(2)}`,
        kind: Math.random() < 0.6 ? 'slime' : 'golem',
        hp: 3,
        position: new THREE.Vector3(x + 0.5, y, z + 0.5),
      };

      mob.mesh = new THREE.Mesh(
        new THREE.BoxGeometry(0.9, 0.9, 0.9),
        new THREE.MeshLambertMaterial({ color: mob.kind === 'slime' ? '#6ecc45' : '#9f5c35' }),
      );
      mob.mesh.position.copy(mob.position).add(new THREE.Vector3(0, 0.55, 0));
      engine.scene.add(mob.mesh);
      engine.mobs.push(mob);
    };

    const updateTarget = () => {
      engine.camera.getWorldDirection(engine.aimDirection);
      const hit = raycastVoxel(engine.camera.position, engine.aimDirection, REACH, engine.edits);
      targetHitRef.current = hit;
      const nextTarget = hit?.block ?? null;
      const prev = targetBlockRef.current;
      const changed = !prev || !nextTarget
        ? prev !== nextTarget
        : prev.x !== nextTarget.x || prev.y !== nextTarget.y || prev.z !== nextTarget.z;

      if (changed) {
        targetBlockRef.current = nextTarget;
        setTargetState(nextTarget);
      }

      if (nextTarget) {
        engine.selection.visible = true;
        engine.selection.position.set(nextTarget.x + 0.5, nextTarget.y + 0.5, nextTarget.z + 0.5);
      } else {
        engine.selection.visible = false;
      }
    };

    const respawn = () => {
      const safeY = terrainHeight(0, 0) + 1.25;
      engine.player.position.set(0, safeY, 0);
      engine.velocity.set(0, 0, 0);
      engine.player.onGround = false;
      healthRef.current = MAX_HEALTH;
      setHealth(MAX_HEALTH);
      setToast('You respawned.');
      rebuildWorld();
    };

    const animate = () => {
      if (!engineRef.current || engine.disposed) {
        return;
      }

      const now = performance.now();
      const delta = Math.min(0.033, (now - engine.lastFrameTime) / 1000 || 0.016);
      engine.lastFrameTime = now;

      engine.timeOfDay = (engine.timeOfDay + delta * DAY_SPEED) % 1;
      const cycle = Math.sin(engine.timeOfDay * Math.PI * 2);
      const daylight = Math.max(0.2, cycle * 0.8 + 0.35);
      engine.background.setRGB(0.08 + daylight * 0.4, 0.12 + daylight * 0.5, 0.18 + daylight * 0.7);
      engine.scene.background = engine.background;
      engine.scene.fog.color.copy(engine.background);
      engine.sun.intensity = 0.6 + daylight * 0.8;
      engine.ambient.intensity = 0.45 + daylight * 0.55;

      engine.camera.rotation.y = engine.controls.yaw;
      engine.camera.rotation.x = engine.controls.pitch;

      engine.camera.getWorldDirection(engine.forward);
      engine.forward.y = 0;
      if (engine.forward.lengthSq() < 0.001) {
        engine.forward.set(0, 0, -1);
      }
      engine.forward.normalize();
      engine.right.crossVectors(engine.forward, engine.up).normalize();

      engine.horizontal.set(0, 0, 0);
      if (engine.keys?.KeyW) engine.horizontal.add(engine.forward);
      if (engine.keys?.KeyS) engine.horizontal.sub(engine.forward);
      if (engine.keys?.KeyA) engine.horizontal.sub(engine.right);
      if (engine.keys?.KeyD) engine.horizontal.add(engine.right);
      if (engine.horizontal.lengthSq() > 0) {
        engine.horizontal.normalize().multiplyScalar(MOVE_SPEED);
      }

      engine.velocity.x = engine.horizontal.x;
      engine.velocity.z = engine.horizontal.z;
      engine.velocity.y -= GRAVITY * delta;

      if (engine.player.onGround && engine.keys?.Space) {
        engine.velocity.y = JUMP_SPEED;
        engine.player.onGround = false;
      }

      const next = engine.player.position.clone();
      next.x += engine.velocity.x * delta;
      if (!collidesWorld(next, PLAYER_SIZE, engine.edits)) {
        engine.player.position.x = next.x;
      }

      next.copy(engine.player.position);
      next.z += engine.velocity.z * delta;
      if (!collidesWorld(next, PLAYER_SIZE, engine.edits)) {
        engine.player.position.z = next.z;
      }

      next.copy(engine.player.position);
      next.y += engine.velocity.y * delta;
      if (!collidesWorld(next, PLAYER_SIZE, engine.edits)) {
        engine.player.position.y = next.y;
        engine.player.onGround = false;
      } else {
        if (engine.velocity.y < 0) {
          engine.player.onGround = true;
        }
        engine.velocity.y = 0;
      }

      if (engine.player.position.y < -16) {
        respawn();
      }

      engine.camera.position.set(
        engine.player.position.x,
        engine.player.position.y + PLAYER_SIZE.eyeHeight,
        engine.player.position.z,
      );

      const nextAnchorX = Math.round(engine.player.position.x);
      const nextAnchorZ = Math.round(engine.player.position.z);
      if (Math.abs(nextAnchorX - anchorRef.current.x) >= 3 || Math.abs(nextAnchorZ - anchorRef.current.z) >= 3) {
        rebuildWorld();
      }

      updateTarget();

      const isNight = engine.timeOfDay > 0.62 || engine.timeOfDay < 0.15;
      engine.mobSpawnAccumulator += delta;
      if (isNight && engine.mobSpawnAccumulator > 3 && engine.mobs.length < 4) {
        engine.mobSpawnAccumulator = 0;
        spawnMob();
      }

      engine.mobs = engine.mobs.filter((mob) => {
        const dx = engine.player.position.x - mob.position.x;
        const dz = engine.player.position.z - mob.position.z;
        const dist = Math.hypot(dx, dz);

        if (isNight && dist > 1.1 && dist < 20) {
          mob.position.x += (dx / dist) * delta * 1.35;
          mob.position.z += (dz / dist) * delta * 1.35;
          mob.position.y = terrainHeight(Math.floor(mob.position.x), Math.floor(mob.position.z)) + 1;
          mob.mesh.position.set(mob.position.x, mob.position.y + 0.55, mob.position.z);
        }

        if (!isNight && dist > 22) {
          engine.scene.remove(mob.mesh);
          return false;
        }

        if (dist < 1.25) {
          const hitNow = performance.now();
          if (!mob.lastHitAt || hitNow - mob.lastHitAt > 1000) {
            mob.lastHitAt = hitNow;
            const nextHealth = Math.max(0, healthRef.current - 1);
            healthRef.current = nextHealth;
            setHealth(nextHealth);
            if (nextHealth <= 0) {
              respawn();
            }
          }
        }

        if (mob.hp <= 0) {
          engine.scene.remove(mob.mesh);
          return false;
        }

        return true;
      });

      engine.hudAccumulator += delta;
      if (engine.hudAccumulator >= HUD_SAMPLE_SECONDS) {
        engine.hudAccumulator = 0;
        setPlayerSample({
          x: engine.player.position.x,
          y: engine.player.position.y,
          z: engine.player.position.z,
          onGround: engine.player.onGround,
        });
        setTimeOfDay(engine.timeOfDay);
      }

      engine.renderer.render(engine.scene, engine.camera);
      engine.animationFrameId = window.requestAnimationFrame(animate);
    };

    const onKeyDown = (event) => {
      engine.keys = engine.keys || {};
      engine.keys[event.code] = true;

      if (/Digit[1-6]/.test(event.code)) {
        setSelectedSlot(Number(event.code.replace('Digit', '')) - 1);
      } else if (event.code === 'KeyC') {
        setShowCrafting((value) => !value);
      }
    };

    const onKeyUp = (event) => {
      engine.keys = engine.keys || {};
      engine.keys[event.code] = false;
    };

    const onMouseMove = (event) => {
      if (document.pointerLockElement !== renderer.domElement) {
        return;
      }
      engine.controls.yaw -= event.movementX * 0.0026;
      engine.controls.pitch -= event.movementY * 0.0022;
      engine.controls.pitch = Math.max(-1.45, Math.min(1.45, engine.controls.pitch));
    };

    const onMouseDown = (event) => {
      if (document.pointerLockElement !== renderer.domElement) {
        return;
      }
      if (event.button === 0) {
        breakTargetBlock();
      } else if (event.button === 2) {
        event.preventDefault();
        placeTargetBlock();
      }
    };

    const onWheel = (event) => {
      setSelectedSlot((current) => {
        const delta = event.deltaY > 0 ? 1 : -1;
        return (current + delta + HOTBAR_ORDER.length) % HOTBAR_ORDER.length;
      });
    };

    const onPointerLockChange = () => {
      const locked = document.pointerLockElement === renderer.domElement;
      engine.pointerLocked = locked;
      setIsLocked(locked);
      if (!locked) {
        setMessage('Cursor unlocked. Click Play to continue.');
      }
    };

    const preventContextMenu = (event) => event.preventDefault();

    window.addEventListener('resize', resize);
    window.addEventListener('keydown', onKeyDown);
    window.addEventListener('keyup', onKeyUp);
    window.addEventListener('mousemove', onMouseMove);
    window.addEventListener('mousedown', onMouseDown);
    window.addEventListener('wheel', onWheel, { passive: true });
    window.addEventListener('contextmenu', preventContextMenu);
    document.addEventListener('pointerlockchange', onPointerLockChange);

    resize();
    animate();

    return () => {
      unmountedRef.current = true;
      if (messageTimerRef.current) {
        window.clearTimeout(messageTimerRef.current);
      }
      window.removeEventListener('resize', resize);
      window.removeEventListener('keydown', onKeyDown);
      window.removeEventListener('keyup', onKeyUp);
      window.removeEventListener('mousemove', onMouseMove);
      window.removeEventListener('mousedown', onMouseDown);
      window.removeEventListener('wheel', onWheel);
      window.removeEventListener('contextmenu', preventContextMenu);
      document.removeEventListener('pointerlockchange', onPointerLockChange);

      if (engineRef.current) {
        const active = engineRef.current;
        active.disposed = true;
        window.cancelAnimationFrame(active.animationFrameId);
        active.scene.traverse((object) => {
          if (object.geometry) {
            object.geometry.dispose?.();
          }
          if (object.material) {
            if (Array.isArray(object.material)) {
              object.material.forEach((mat) => mat.dispose?.());
            } else {
              object.material.dispose?.();
            }
          }
        });
        Object.values(active.materials).forEach((mat) => {
          mat.map?.dispose?.();
          mat.dispose?.();
        });
        active.renderer.dispose();
        if (active.renderer.domElement.parentNode === mount) {
          mount.removeChild(active.renderer.domElement);
        }
      }
      engineRef.current = null;
    };
  }, [breakTargetBlock, placeTargetBlock, rebuildWorld, setToast]);

  return (
    <div className="game-shell">
      <div ref={mountRef} className="game-canvas" />

      <div className="hud top-left">
        <div className="panel title-panel">
          <h1>MiniCraft 3D</h1>
          <p>A lighter, more reliable Minecraft-style browser prototype.</p>
        </div>
        <div className="panel stats-panel">
          <div><strong>Health:</strong> <span className="hearts">{hearts(health)}</span></div>
          <div><strong>Coords:</strong> {playerSample.x.toFixed(1)}, {playerSample.y.toFixed(1)}, {playerSample.z.toFixed(1)}</div>
          <div><strong>Time:</strong> {timeOfDay > 0.62 || timeOfDay < 0.15 ? 'Night' : 'Day'}</div>
          <div><strong>Target:</strong> {targetState ? `${targetState.x}, ${targetState.y}, ${targetState.z}` : 'None'}</div>
        </div>
      </div>

      <div className="hud top-right">
        <div className="panel controls-panel">
          <h2>Controls</h2>
          <ul>
            <li>WASD move</li>
            <li>Space jump</li>
            <li>Left click break / hit</li>
            <li>Right click place</li>
            <li>1-6 or mouse wheel switch block</li>
            <li>C toggle crafting</li>
            <li>Esc unlock cursor</li>
          </ul>
          <button className="play-button" onClick={requestPlay}>{isLocked ? 'Playing' : 'Play'}</button>
        </div>
        {showCrafting ? (
          <div className="panel crafting-panel">
            <h2>Crafting</h2>
            {RECIPES.map((recipe) => (
              <button key={recipe.id} className="craft-btn" disabled={!canCraft(recipe, inventory)} onClick={() => craftRecipe(recipe)}>
                <span>{recipe.name}</span>
                <small>{recipe.description}</small>
              </button>
            ))}
          </div>
        ) : null}
      </div>

      <div className="hud bottom-left">
        <div className="panel inventory-panel">
          <h2>Inventory</h2>
          <div className="inventory-grid">
            {HOTBAR_ORDER.concat(BLOCK.LEAVES).map((blockId) => (
              <div key={blockId} className="inventory-row">
                <span>{blockLabel(blockId)}</span>
                <strong>{inventory[blockId] ?? 0}</strong>
              </div>
            ))}
          </div>
        </div>
      </div>

      <div className="hud bottom-center">
        <div className="hotbar">
          {HOTBAR_ORDER.map((blockId, index) => (
            <button key={blockId} className={`slot ${index === selectedSlot ? 'selected' : ''}`} onClick={() => setSelectedSlot(index)}>
              <span>{index + 1}</span>
              <strong>{blockLabel(blockId)}</strong>
              <em>{inventory[blockId] ?? 0}</em>
            </button>
          ))}
        </div>
        <div className="crosshair" />
        {message ? <div className="message">{message}</div> : null}
      </div>
    </div>
  );
}
