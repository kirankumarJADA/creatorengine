import { useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import * as THREE from 'three';

import { APP_NAME, ROUTES } from '../utils/constants.js';

const CAM_POINTS = [
  [0, 0.4, 9],
  [0, 0.1, 4.2],
  [0, -3.5, 0],
  [0, -8, -4],
  [2.5, -13, -10],
  [-2, -18, -16],
  [0, -23, -22],
  [0, -27, -28],
  [0, -20, -34],
].map((p) => new THREE.Vector3(...p));

const LOOK_POINTS = [
  [0, 0.4, 0],
  [0, -0.4, 0],
  [0, -8, -4],
  [0, -13, -10],
  [0, -18, -16],
  [0, -23, -22],
  [0, -27, -28],
  [0, -22, -34],
  [0, -14, -38],
].map((p) => new THREE.Vector3(...p));

const camCurve = new THREE.CatmullRomCurve3(CAM_POINTS, false, 'centripetal', 0.4);
const lookCurve = new THREE.CatmullRomCurve3(LOOK_POINTS, false, 'centripetal', 0.4);

const SECTIONS = [
  {
    kicker: 'Every comment is a signal',
    title: 'It starts with a comment.',
    text: '“link” · “price” · “how?” — thousands of intents, buried in your notifications. Scroll to see where they go.',
    hint: true,
  },
  {
    kicker: 'Beneath the interface',
    title: 'Welcome to the engine.',
    text: 'Every comment becomes a packet on the rail — captured the instant it lands, 24/7, while you create.',
    align: 'right',
  },
  {
    kicker: 'Stage 01 · Detection',
    title: 'Keywords, scanned in real time.',
    text: 'Exact match or contains — your triggers decide which packets pass the gate.',
  },
  {
    kicker: 'Stage 02 · Routing',
    title: 'Sorted. Gated. Qualified.',
    text: 'Follow-gates and conditions flip the tracks — only the right fans continue through.',
    align: 'right',
  },
  {
    kicker: 'Stage 03 · Assembly',
    title: 'Your DM, built to order.',
    text: 'Message, link, and tone assembled per fan — personal at any scale.',
  },
  {
    kicker: 'Stage 04 · Delivery',
    title: 'Fired back to the surface.',
    text: 'Delivered in seconds, straight to their inbox — before their attention moves on.',
    align: 'right',
  },
];

const Home = () => {
  const canvasRef = useRef(null);
  const scrollWrapRef = useRef(null);
  const pointerRef = useRef({ x: 0, y: 0 });
  const scrollRef = useRef(0);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return undefined;

    const renderer = new THREE.WebGLRenderer({
      canvas,
      antialias: true,
      alpha: false,
      powerPreference: 'high-performance',
    });
    renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 1.8));

    const scene = new THREE.Scene();
    scene.background = new THREE.Color(0x050508);
    scene.fog = new THREE.Fog(0x050508, 8, 42);

    const camera = new THREE.PerspectiveCamera(55, 1, 0.1, 90);
    camera.position.copy(CAM_POINTS[0]);

    const metal = new THREE.MeshStandardMaterial({ color: 0x1a1d24, metalness: 0.9, roughness: 0.25 });
    const darkMetal = new THREE.MeshStandardMaterial({ color: 0x0c0e13, metalness: 0.85, roughness: 0.4 });
    const brand = new THREE.MeshStandardMaterial({
      color: 0x6366f1, metalness: 0.6, roughness: 0.3,
      emissive: 0x4338ca, emissiveIntensity: 0.35,
    });
    const glowCore = new THREE.MeshBasicMaterial({ color: 0xa5b4fc });
    const glass = new THREE.MeshPhysicalMaterial({
      color: 0x0f172a, metalness: 0, roughness: 0.05,
      transmission: 0.9, thickness: 0.5, transparent: true, opacity: 0.35,
    });

    const post = new THREE.Group();
    post.position.set(0, 0.4, 0);
    const frame = new THREE.Mesh(new THREE.BoxGeometry(3.2, 4.4, 0.12), darkMetal);
    post.add(frame);
    const screen = new THREE.Mesh(
      new THREE.PlaneGeometry(2.9, 3.2),
      new THREE.MeshStandardMaterial({ color: 0x12141c, roughness: 0.6 })
    );
    screen.position.set(0, 0.3, 0.07);
    post.add(screen);
    const postImg = new THREE.Mesh(
      new THREE.PlaneGeometry(2.7, 2.1),
      new THREE.MeshStandardMaterial({ color: 0x312e81, emissive: 0x4338ca, emissiveIntensity: 0.15, roughness: 0.5 })
    );
    postImg.position.set(0, 0.65, 0.08);
    post.add(postImg);

    const commentPortal = new THREE.Group();
    commentPortal.position.set(0, -0.9, 0.12);
    const commentShell = new THREE.Mesh(new THREE.CapsuleGeometry(0.16, 1.9, 8, 16), brand);
    commentPortal.add(commentShell);
    const commentGlow = new THREE.Mesh(new THREE.CapsuleGeometry(0.16, 1.9, 8, 16), glowCore);
    commentGlow.scale.set(0.5, 0.5, 0.5);
    commentPortal.add(commentGlow);
    const commentLight = new THREE.PointLight(0x818cf8, 4, 3);
    commentPortal.add(commentLight);
    post.add(commentPortal);
    scene.add(post);

    const hall = new THREE.Group();
    [-8, -13, -18, -23, -27].forEach((y, i) => {
      const floor = new THREE.Mesh(new THREE.PlaneGeometry(26, 14), darkMetal);
      floor.rotation.x = -Math.PI / 2;
      floor.position.set(0, y - 2.2, -4 - i * 6);
      hall.add(floor);
      [-9, 9].forEach((x) => {
        const col = new THREE.Mesh(new THREE.CylinderGeometry(0.35, 0.45, 7, 10), metal);
        col.position.set(x, y - 2.2 + 3.5, -4 - i * 6);
        hall.add(col);
      });
      const strip = new THREE.Mesh(
        new THREE.PlaneGeometry(18, 0.4),
        new THREE.MeshBasicMaterial({ color: 0x6366f1, transparent: true, opacity: 0.65 })
      );
      strip.rotation.x = Math.PI / 2;
      strip.position.set(0, y - 2.2 + 6.8, -4 - i * 6);
      hall.add(strip);
    });
    scene.add(hall);

    const packetPools = [];
    const buildRail = (pointsArr, count, color) => {
      const curve = new THREE.CatmullRomCurve3(pointsArr.map((p) => new THREE.Vector3(...p)));
      const tubeGeo = new THREE.TubeGeometry(curve, 80, 0.05, 8, false);
      scene.add(new THREE.Mesh(tubeGeo, metal));
      const innerGeo = new THREE.TubeGeometry(curve, 80, 0.018, 6, false);
      scene.add(new THREE.Mesh(innerGeo, new THREE.MeshBasicMaterial({ color, transparent: true, opacity: 0.5 })));

      const packets = [];
      for (let i = 0; i < count; i += 1) {
        const g = new THREE.Group();
        const body = new THREE.Mesh(
          new THREE.CapsuleGeometry(0.07, 0.22, 6, 10),
          new THREE.MeshBasicMaterial({ color })
        );
        g.add(body);
        const light = new THREE.PointLight(color, 1.2, 1.6);
        g.add(light);
        scene.add(g);
        packets.push({ group: g, offset: i / count });
      }
      packetPools.push({ curve, packets, speed: 0.12 });
    };

    buildRail(
      [[0, -1.6, 0], [0, -6, -2], [1.5, -10, -6], [2.5, -13, -10],
       [0.5, -16, -13], [-2, -18, -16], [-1, -21, -19], [0, -23.4, -22]],
      8, 0xa5b4fc
    );
    buildRail([[-6, -9, -3], [-5, -14, -9], [-6, -19, -15], [-5, -24, -21]], 3, 0x312e81);
    buildRail([[6, -8, -4], [5, -13, -10], [6, -18, -16], [5, -23, -22]], 3, 0x312e81);

    const scannerGroup = new THREE.Group();
    scannerGroup.position.set(2.5, -13, -10);
    const ringMat = new THREE.MeshStandardMaterial({
      color: 0x6366f1, metalness: 0.7, roughness: 0.2,
      emissive: 0x6366f1, emissiveIntensity: 0.6,
    });
    const ring = new THREE.Mesh(new THREE.TorusGeometry(1.6, 0.14, 12, 48), ringMat);
    ring.rotation.x = Math.PI / 2;
    scannerGroup.add(ring);
    for (let i = 0; i < 4; i += 1) {
      const strut = new THREE.Mesh(new THREE.BoxGeometry(0.9, 0.18, 0.18), metal);
      const a = (i * Math.PI) / 2;
      strut.position.set(Math.cos(a) * 2.1, Math.sin(a) * 2.1, 0);
      strut.rotation.z = a;
      scannerGroup.add(strut);
    }
    const scannerLight = new THREE.PointLight(0x6366f1, 6, 8);
    scannerGroup.add(scannerLight);
    scene.add(scannerGroup);

    const gateGroup = new THREE.Group();
    gateGroup.position.set(-2, -18, -16);
    const gateBase = new THREE.Mesh(new THREE.BoxGeometry(3.4, 0.3, 1.6), darkMetal);
    gateBase.position.set(0, -0.6, 0);
    gateGroup.add(gateBase);
    const flap = new THREE.Mesh(new THREE.BoxGeometry(1.8, 0.12, 0.5), brand);
    gateGroup.add(flap);
    const lampGreen = new THREE.Mesh(
      new THREE.SphereGeometry(0.1, 12, 12),
      new THREE.MeshBasicMaterial({ color: 0x34d399 })
    );
    lampGreen.position.set(1.4, 0.4, 0);
    gateGroup.add(lampGreen);
    const lampRed = new THREE.Mesh(
      new THREE.SphereGeometry(0.1, 12, 12),
      new THREE.MeshBasicMaterial({ color: 0xf87171 })
    );
    lampRed.position.set(-1.4, 0.4, 0);
    gateGroup.add(lampRed);
    const gateLight = new THREE.PointLight(0x34d399, 2, 4);
    gateLight.position.set(1.4, 0.4, 0);
    gateGroup.add(gateLight);
    scene.add(gateGroup);

    const asmGroup = new THREE.Group();
    asmGroup.position.set(0, -23, -22);
    const asmBar = new THREE.Mesh(new THREE.BoxGeometry(4.2, 0.35, 1), darkMetal);
    asmBar.position.set(0, 1.4, 0);
    asmGroup.add(asmBar);

    const makeArm = (side) => {
      const g = new THREE.Group();
      g.position.set(side * 1.6, 0.8, 0);
      const bone = new THREE.Mesh(new THREE.BoxGeometry(1.4, 0.16, 0.16), metal);
      bone.position.set(side * -0.5, -0.5, 0);
      bone.rotation.z = side * 0.7;
      g.add(bone);
      const joint = new THREE.Mesh(new THREE.SphereGeometry(0.13, 12, 12), brand);
      joint.position.set(side * -1.05, -0.95, 0);
      g.add(joint);
      return g;
    };
    const armL = makeArm(-1);
    const armR = makeArm(1);
    asmGroup.add(armL, armR);

    const dmSlab = new THREE.Group();
    dmSlab.position.set(0, -0.4, 0);
    const slabBody = new THREE.Mesh(new THREE.BoxGeometry(1.7, 0.9, 0.22), brand);
    dmSlab.add(slabBody);
    const slabGlow = new THREE.Mesh(new THREE.BoxGeometry(1.7, 0.9, 0.18), glowCore);
    slabGlow.scale.set(0.92, 0.8, 1.2);
    dmSlab.add(slabGlow);
    asmGroup.add(dmSlab);

    const asmLight = new THREE.PointLight(0x818cf8, 5, 7);
    asmGroup.add(asmLight);
    scene.add(asmGroup);

    const tubeGroup = new THREE.Group();
    tubeGroup.position.set(0, -27, -28);
    const launchCurve = new THREE.CatmullRomCurve3([
      new THREE.Vector3(0, -2, 0),
      new THREE.Vector3(0, 4, -1),
      new THREE.Vector3(0, 12, -4),
    ]);
    tubeGroup.add(new THREE.Mesh(new THREE.TubeGeometry(launchCurve, 40, 0.5, 16, false), glass));
    tubeGroup.add(new THREE.Mesh(new THREE.TubeGeometry(launchCurve, 40, 0.56, 16, false), metal));
    const launchPackets = [];
    for (let i = 0; i < 5; i += 1) {
      const g = new THREE.Group();
      const body = new THREE.Mesh(
        new THREE.CapsuleGeometry(0.07, 0.22, 6, 10),
        new THREE.MeshBasicMaterial({ color: 0x34d399 })
      );
      g.add(body);
      const light = new THREE.PointLight(0x34d399, 1.2, 1.6);
      g.add(light);
      tubeGroup.add(g);
      launchPackets.push({ group: g, offset: i / 5 });
    }
    const tubeLight = new THREE.PointLight(0x34d399, 4, 10);
    tubeLight.position.set(0, 4, -1);
    tubeGroup.add(tubeLight);
    scene.add(tubeGroup);

    scene.add(new THREE.AmbientLight(0x1a1d24, 0.5));
    const keyLight = new THREE.DirectionalLight(0xc7d2fe, 0.7);
    keyLight.position.set(6, 4, 8);
    scene.add(keyLight);

    const resize = () => {
      const { clientWidth, clientHeight } = canvas;
      renderer.setSize(clientWidth, clientHeight, false);
      camera.aspect = clientWidth / Math.max(clientHeight, 1);
      camera.updateProjectionMatrix();
    };

    const onPointerMove = (event) => {
      pointerRef.current.x = (event.clientX / window.innerWidth - 0.5) * 2;
      pointerRef.current.y = (event.clientY / window.innerHeight - 0.5) * 2;
    };

    const onScroll = () => {
      const wrap = scrollWrapRef.current;
      if (!wrap) return;
      const max = wrap.scrollHeight - window.innerHeight;
      scrollRef.current = max > 0 ? Math.min(1, Math.max(0, window.scrollY / max)) : 0;
    };

    const lookTarget = new THREE.Vector3();
    let frameId = 0;
    const clock = new THREE.Clock();

    const animate = () => {
      const elapsed = clock.getElapsedTime();
      const t = scrollRef.current;
      const pointer = pointerRef.current;

      camCurve.getPointAt(t, camera.position);
      lookCurve.getPointAt(t, lookTarget);
      camera.position.x += pointer.x * 0.3;
      camera.position.y += pointer.y * 0.18;
      camera.lookAt(lookTarget);

      commentPortal.position.y = -0.9 + Math.sin(elapsed * 2) * 0.04;

      packetPools.forEach(({ curve, packets, speed }) => {
        packets.forEach((p) => {
          const pt = (elapsed * speed + p.offset) % 1;
          curve.getPointAt(pt, p.group.position);
        });
      });
      launchPackets.forEach((p) => {
        const pt = (elapsed * 0.25 + p.offset) % 1;
        launchCurve.getPointAt(pt, p.group.position);
      });

      ring.rotation.z = elapsed * 0.6;
      ringMat.emissiveIntensity = 0.5 + Math.sin(elapsed * 4) * 0.3;

      flap.rotation.y = Math.sin(elapsed * 1.2) > 0 ? 0.5 : -0.5;

      armL.rotation.z = -0.4 + Math.sin(elapsed * 2.2) * 0.35;
      armR.rotation.z = 0.4 - Math.sin(elapsed * 2.2 + 0.5) * 0.35;
      const s = 0.6 + ((Math.sin(elapsed * 1.1) + 1) / 2) * 0.4;
      dmSlab.scale.set(s, s, s);

      renderer.render(scene, camera);
      frameId = window.requestAnimationFrame(animate);
    };

    resize();
    onScroll();
    window.addEventListener('resize', resize);
    window.addEventListener('pointermove', onPointerMove);
    window.addEventListener('scroll', onScroll, { passive: true });
    animate();

    return () => {
      window.cancelAnimationFrame(frameId);
      window.removeEventListener('resize', resize);
      window.removeEventListener('pointermove', onPointerMove);
      window.removeEventListener('scroll', onScroll);
      renderer.dispose();

      scene.traverse((obj) => {
        if (obj.geometry) obj.geometry.dispose();
        if (obj.material) {
          if (Array.isArray(obj.material)) obj.material.forEach((m) => m.dispose());
          else obj.material.dispose();
        }
      });
    };
  }, []);

  return (
    <main className="relative bg-[#050508] text-white">
      <div className="fixed inset-0 z-0">
        <canvas ref={canvasRef} className="h-full w-full" aria-hidden="true" />
        <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_50%_0%,rgba(99,102,241,0.10),transparent_45%)]" />
      </div>

      <nav className="pointer-events-none fixed inset-x-0 top-0 z-20 flex items-center justify-between px-6 py-6 lg:px-10">
        <Link to="/" className="pointer-events-auto inline-flex items-center gap-2.5">
          <img src="/logo-mark.png" alt={APP_NAME} className="h-9 w-9 object-contain" />
          <span className="text-base font-semibold tracking-tight">{APP_NAME}</span>
        </Link>
        <div className="pointer-events-auto flex items-center gap-3">
          <Link
            to={ROUTES.LOGIN}
            className="rounded-xl px-4 py-2 text-sm font-medium text-ink-300 transition-colors hover:text-white"
          >
            Sign in
          </Link>
          <Link
            to={ROUTES.REGISTER}
            className="rounded-xl bg-white px-4 py-2 text-sm font-semibold text-ink-950 shadow-elevated transition-transform hover:-translate-y-0.5"
          >
            Get started
          </Link>
        </div>
      </nav>

      <div ref={scrollWrapRef} className="relative z-10">
        {SECTIONS.map((s) => (
          <section
            key={s.title}
            className={`flex h-screen items-center px-8 sm:px-16 ${s.align === 'right' ? 'justify-end text-right' : 'justify-start'}`}
          >
            <div className="max-w-md">
              <p className="font-mono text-[11px] uppercase tracking-[0.3em] text-brand-400">{s.kicker}</p>
              <h2 className="mt-3 font-display text-4xl leading-tight text-white sm:text-5xl">{s.title}</h2>
              <p className="mt-4 text-[15px] leading-relaxed text-ink-400">{s.text}</p>
              {s.hint && (
                <p className="mt-8 animate-bounce font-mono text-xs text-ink-500">↓ scroll to dive in</p>
              )}
            </div>
          </section>
        ))}

        <section className="flex h-screen flex-col items-center justify-center px-8 text-center">
          <p className="font-mono text-[11px] uppercase tracking-[0.3em] text-brand-400">
            Comment → AI → Automation → DM
          </p>
          <h2 className="mt-4 font-display text-5xl leading-tight text-white sm:text-6xl">
            You, at scale.
          </h2>
          <p className="mt-4 max-w-md text-ink-400">
            The engine runs while you create. Set your first automation in minutes.
          </p>
          <div className="mt-8 flex items-center gap-4">
            <Link
              to={ROUTES.REGISTER}
              className="rounded-xl bg-brand-600 px-7 py-3.5 text-sm font-semibold text-white shadow-elevated transition-transform hover:-translate-y-0.5"
            >
              Start free
            </Link>
            <Link
              to={ROUTES.LOGIN}
              className="rounded-xl border border-ink-700 px-7 py-3.5 text-sm font-medium text-ink-200 transition-colors hover:border-ink-500"
            >
              Sign in
            </Link>
          </div>
        </section>
      </div>
    </main>
  );
};

export default Home;