/* eslint-disable react/no-unknown-property */
import { useMemo, useRef } from 'react';
import { Link } from 'react-router-dom';
import * as THREE from 'three';
import { Canvas, useFrame } from '@react-three/fiber';
import {
  ScrollControls, useScroll, Scroll,
  Environment, Float,
} from '@react-three/drei';
import { EffectComposer, Bloom, Vignette } from '@react-three/postprocessing';
import { ROUTES } from '../utils/constants.js';

/* ────────────────────────────────────────────────────────────
   CAMERA PATH — one continuous dive:
   surface post → through comment → rail hall → AI scanner →
   sorting gates → DM assembler → launch tube → delivered
   ──────────────────────────────────────────────────────────── */
const CAM_POINTS = [
  new THREE.Vector3(0, 0.4, 9),      // 0 facing the IG post
  new THREE.Vector3(0, 0.1, 4.2),    // 1 approaching comment
  new THREE.Vector3(0, -3.5, 0),     // 2 diving through
  new THREE.Vector3(0, -8, -4),      // 3 rail hall
  new THREE.Vector3(2.5, -13, -10),  // 4 AI scanner
  new THREE.Vector3(-2, -18, -16),   // 5 sorting gates
  new THREE.Vector3(0, -23, -22),    // 6 DM assembler
  new THREE.Vector3(0, -27, -28),    // 7 launch tube base
  new THREE.Vector3(0, -20, -34),    // 8 riding up / delivered
];
const camCurve = new THREE.CatmullRomCurve3(CAM_POINTS, false, 'centripetal', 0.4);

const LOOK_POINTS = [
  new THREE.Vector3(0, 0.4, 0),
  new THREE.Vector3(0, -0.4, 0),
  new THREE.Vector3(0, -8, -4),
  new THREE.Vector3(0, -13, -10),
  new THREE.Vector3(0, -18, -16),
  new THREE.Vector3(0, -23, -22),
  new THREE.Vector3(0, -27, -28),
  new THREE.Vector3(0, -22, -34),
  new THREE.Vector3(0, -14, -38),
];
const lookCurve = new THREE.CatmullRomCurve3(LOOK_POINTS, false, 'centripetal', 0.4);

const CameraRig = () => {
  const scroll = useScroll();
  const look = useRef(new THREE.Vector3());
  useFrame(({ camera, pointer }) => {
    const t = THREE.MathUtils.clamp(scroll.offset, 0, 1);
    camCurve.getPointAt(t, camera.position);
    lookCurve.getPointAt(t, look.current);
    // subtle parallax from mouse
    camera.position.x += pointer.x * 0.35;
    camera.position.y += pointer.y * 0.2;
    camera.lookAt(look.current);
  });
  return null;
};

/* ── Materials (shared, premium industrial) ── */
const useMats = () =>
  useMemo(() => ({
    metal: new THREE.MeshStandardMaterial({ color: '#1a1d24', metalness: 0.9, roughness: 0.25 }),
    darkMetal: new THREE.MeshStandardMaterial({ color: '#0c0e13', metalness: 0.85, roughness: 0.4 }),
    brand: new THREE.MeshStandardMaterial({ color: '#6366f1', metalness: 0.6, roughness: 0.3, emissive: '#4338ca', emissiveIntensity: 0.35 }),
    glowCore: new THREE.MeshBasicMaterial({ color: '#a5b4fc' }),
    glowGreen: new THREE.MeshBasicMaterial({ color: '#34d399' }),
    glass: new THREE.MeshPhysicalMaterial({ color: '#0f172a', metalness: 0, roughness: 0.05, transmission: 0.9, thickness: 0.5, transparent: true, opacity: 0.35 }),
  }), []);

/* ── ACT 1: the Instagram post at the surface ── */
const SurfacePost = ({ mats }) => (
  <group position={[0, 0.4, 0]}>
    {/* phone-like frame */}
    <mesh material={mats.darkMetal}>
      <boxGeometry args={[3.2, 4.4, 0.12]} />
    </mesh>
    {/* screen */}
    <mesh position={[0, 0.3, 0.07]}>
      <planeGeometry args={[2.9, 3.2]} />
      <meshStandardMaterial color="#12141c" roughness={0.6} />
    </mesh>
    {/* fake post image block */}
    <mesh position={[0, 0.65, 0.08]}>
      <planeGeometry args={[2.7, 2.1]} />
      <meshStandardMaterial color="#312e81" emissive="#4338ca" emissiveIntensity={0.15} roughness={0.5} />
    </mesh>
    {/* the comment — a glowing pill, the portal */}
    <Float speed={2} floatIntensity={0.15} rotationIntensity={0}>
      <group position={[0, -0.9, 0.12]}>
        <mesh material={mats.brand}>
          <capsuleGeometry args={[0.16, 1.9, 8, 16]} />
        </mesh>
        <mesh material={mats.glowCore} scale={[0.5, 0.5, 0.5]}>
          <capsuleGeometry args={[0.16, 1.9, 8, 16]} />
        </mesh>
        <pointLight color="#818cf8" intensity={4} distance={3} />
      </group>
    </Float>
  </group>
);

/* ── Data packet: glowing capsule that travels a curve ── */
const Packet = ({ curve, speed = 0.12, offset = 0, color = '#a5b4fc' }) => {
  const ref = useRef();
  const pos = useRef(new THREE.Vector3());
  useFrame(({ clock }) => {
    const t = (clock.elapsedTime * speed + offset) % 1;
    curve.getPointAt(t, pos.current);
    ref.current.position.copy(pos.current);
  });
  return (
    <group ref={ref}>
      <mesh>
        <capsuleGeometry args={[0.07, 0.22, 6, 10]} />
        <meshBasicMaterial color={color} />
      </mesh>
      <pointLight color={color} intensity={1.2} distance={1.6} />
    </group>
  );
};

/* ── Rail: metallic tube following a curve, with packets ── */
const Rail = ({ points, mats, packets = 4, color }) => {
  const curve = useMemo(() => new THREE.CatmullRomCurve3(points.map((p) => new THREE.Vector3(...p))), [points]);
  return (
    <group>
      <mesh material={mats.metal}>
        <tubeGeometry args={[curve, 80, 0.05, 8, false]} />
      </mesh>
      {/* inner light guide */}
      <mesh>
        <tubeGeometry args={[curve, 80, 0.018, 6, false]} />
        <meshBasicMaterial color={color || '#4338ca'} transparent opacity={0.5} />
      </mesh>
      {Array.from({ length: packets }).map((_, i) => (
        <Packet key={i} curve={curve} offset={i / packets} color={color || '#a5b4fc'} />
      ))}
    </group>
  );
};

/* ── ACT 3: AI scanner ring — packets pass through, ring pulses ── */
const ScannerStation = ({ mats, position }) => {
  const ring = useRef();
  useFrame(({ clock }) => {
    ring.current.rotation.z = clock.elapsedTime * 0.6;
    ring.current.material.emissiveIntensity = 0.5 + Math.sin(clock.elapsedTime * 4) * 0.3;
  });
  return (
    <group position={position}>
      <mesh ref={ring} rotation={[Math.PI / 2, 0, 0]}>
        <torusGeometry args={[1.6, 0.14, 12, 48]} />
        <meshStandardMaterial color="#6366f1" metalness={0.7} roughness={0.2} emissive="#6366f1" emissiveIntensity={0.6} />
      </mesh>
      {/* scan beam plane */}
      <mesh rotation={[0, 0, 0]}>
        <circleGeometry args={[1.5, 32]} />
        <meshBasicMaterial color="#818cf8" transparent opacity={0.08} side={THREE.DoubleSide} />
      </mesh>
      {/* housing struts */}
      {[0, 1, 2, 3].map((i) => (
        <mesh key={i} material={mats.metal} position={[Math.cos((i * Math.PI) / 2) * 2.1, Math.sin((i * Math.PI) / 2) * 2.1, 0]} rotation={[0, 0, (i * Math.PI) / 2]}>
          <boxGeometry args={[0.9, 0.18, 0.18]} />
        </mesh>
      ))}
      <pointLight color="#6366f1" intensity={6} distance={8} />
    </group>
  );
};

/* ── ACT 4: sorting gate — a switch-track flipper ── */
const SortingGate = ({ mats, position }) => {
  const flap = useRef();
  useFrame(({ clock }) => {
    // periodically flip the switch track
    flap.current.rotation.y = Math.sin(clock.elapsedTime * 1.2) > 0 ? 0.5 : -0.5;
  });
  return (
    <group position={position}>
      <mesh material={mats.darkMetal} position={[0, -0.6, 0]}>
        <boxGeometry args={[3.4, 0.3, 1.6]} />
      </mesh>
      <mesh ref={flap} material={mats.brand} position={[0, 0, 0]}>
        <boxGeometry args={[1.8, 0.12, 0.5]} />
      </mesh>
      {/* two exit lamps: match / no-match */}
      <mesh position={[1.4, 0.4, 0]} material={mats.glowGreen}>
        <sphereGeometry args={[0.1, 12, 12]} />
      </mesh>
      <mesh position={[-1.4, 0.4, 0]}>
        <sphereGeometry args={[0.1, 12, 12]} />
        <meshBasicMaterial color="#f87171" />
      </mesh>
      <pointLight color="#34d399" intensity={2} distance={4} position={[1.4, 0.4, 0]} />
    </group>
  );
};

/* ── ACT 5: DM assembler — mechanical arms + growing message ── */
const Assembler = ({ mats, position }) => {
  const armL = useRef();
  const armR = useRef();
  const msg = useRef();
  useFrame(({ clock }) => {
    const t = clock.elapsedTime;
    armL.current.rotation.z = -0.4 + Math.sin(t * 2.2) * 0.35;
    armR.current.rotation.z = 0.4 - Math.sin(t * 2.2 + 0.5) * 0.35;
    const s = 0.6 + ((Math.sin(t * 1.1) + 1) / 2) * 0.4;
    msg.current.scale.set(s, s, s);
  });
  const Arm = ({ side, refFn }) => (
    <group ref={refFn} position={[side * 1.6, 0.8, 0]}>
      <mesh material={mats.metal} position={[side * -0.5, -0.5, 0]} rotation={[0, 0, side * 0.7]}>
        <boxGeometry args={[1.4, 0.16, 0.16]} />
      </mesh>
      <mesh material={mats.brand} position={[side * -1.05, -0.95, 0]}>
        <sphereGeometry args={[0.13, 12, 12]} />
      </mesh>
    </group>
  );
  return (
    <group position={position}>
      <mesh material={mats.darkMetal} position={[0, 1.4, 0]}>
        <boxGeometry args={[4.2, 0.35, 1]} />
      </mesh>
      <Arm side={-1} refFn={armL} />
      <Arm side={1} refFn={armR} />
      {/* the DM being assembled — glowing rounded slab */}
      <group ref={msg} position={[0, -0.4, 0]}>
        <mesh material={mats.brand}>
          <boxGeometry args={[1.7, 0.9, 0.22]} />
        </mesh>
        <mesh material={mats.glowCore} scale={[0.92, 0.8, 1.2]}>
          <boxGeometry args={[1.7, 0.9, 0.18]} />
        </mesh>
      </group>
      <pointLight color="#818cf8" intensity={5} distance={7} />
    </group>
  );
};

/* ── ACT 6: launch tube firing DMs back to the surface ── */
const LaunchTube = ({ mats, position }) => {
  const curve = useMemo(
    () => new THREE.CatmullRomCurve3([
      new THREE.Vector3(0, -2, 0),
      new THREE.Vector3(0, 4, -1),
      new THREE.Vector3(0, 12, -4),
    ]),
    []
  );
  return (
    <group position={position}>
      <mesh material={mats.glass}>
        <tubeGeometry args={[curve, 40, 0.5, 16, false]} />
      </mesh>
      <mesh material={mats.metal}>
        <tubeGeometry args={[curve, 40, 0.56, 16, false]} />
      </mesh>
      {Array.from({ length: 5 }).map((_, i) => (
        <Packet key={i} curve={curve} speed={0.25} offset={i / 5} color="#34d399" />
      ))}
      <pointLight color="#34d399" intensity={4} distance={10} position={[0, 4, -1]} />
    </group>
  );
};

/* ── Industrial hall shell: floor grid + columns for believability ── */
const Hall = ({ mats }) => (
  <group>
    {[-8, -13, -18, -23, -27].map((y, i) => (
      <group key={y} position={[0, y - 2.2, -4 - i * 6]}>
        <mesh material={mats.darkMetal} rotation={[-Math.PI / 2, 0, 0]}>
          <planeGeometry args={[26, 14]} />
        </mesh>
        {/* columns */}
        {[-9, 9].map((x) => (
          <mesh key={x} material={mats.metal} position={[x, 3.5, 0]}>
            <cylinderGeometry args={[0.35, 0.45, 7, 10]} />
          </mesh>
        ))}
        {/* ceiling strip light */}
        <mesh position={[0, 6.8, 0]} rotation={[Math.PI / 2, 0, 0]}>
          <planeGeometry args={[18, 0.4]} />
          <meshBasicMaterial color="#6366f1" transparent opacity={0.65} />
        </mesh>
      </group>
    ))}
  </group>
);

/* ── HTML overlay copy per act — minimal text, story-driven ── */
const Overlay = () => {
  const Section = ({ children, align = 'left' }) => (
    <section className={`flex h-screen items-center px-8 sm:px-16 ${align === 'right' ? 'justify-end' : 'justify-start'}`}>
      <div className="max-w-md">{children}</div>
    </section>
  );
  const Kicker = ({ children }) => (
    <p className="font-mono text-[11px] uppercase tracking-[0.3em] text-brand-400">{children}</p>
  );
  const H = ({ children }) => (
    <h2 className="mt-3 font-display text-4xl leading-tight text-white sm:text-5xl">{children}</h2>
  );
  const P = ({ children }) => (
    <p className="mt-4 text-[15px] leading-relaxed text-ink-400">{children}</p>
  );
  return (
    <>
      <Section>
        <Kicker>Every comment is a signal</Kicker>
        <H>It starts with a comment.</H>
        <P>“link” · “price” · “how?” — thousands of intents, buried in your notifications. Scroll to see where they go.</P>
        <p className="mt-8 animate-bounce font-mono text-xs text-ink-500">↓ scroll to dive in</p>
      </Section>
      <Section align="right">
        <Kicker>Beneath the interface</Kicker>
        <H>Welcome to the engine.</H>
        <P>Every comment becomes a packet on the rail — captured the instant it lands, 24/7, while you create.</P>
      </Section>
      <Section>
        <Kicker>Stage 01 · Detection</Kicker>
        <H>Keywords, scanned in real time.</H>
        <P>Exact match or contains — your triggers decide which packets pass the gate.</P>
      </Section>
      <Section align="right">
        <Kicker>Stage 02 · Routing</Kicker>
        <H>Sorted. Gated. Qualified.</H>
        <P>Follow-gates and conditions flip the tracks — only the right fans continue through.</P>
      </Section>
      <Section>
        <Kicker>Stage 03 · Assembly</Kicker>
        <H>Your DM, built to order.</H>
        <P>Message, link, and tone assembled per fan — personal at any scale.</P>
      </Section>
      <Section align="right">
        <Kicker>Stage 04 · Delivery</Kicker>
        <H>Fired back to the surface.</H>
        <P>Delivered in seconds, straight to their inbox — before their attention moves on.</P>
      </Section>
      <section className="flex h-screen flex-col items-center justify-center px-8 text-center">
        <Kicker>Comment → AI → Automation → DM</Kicker>
        <h2 className="mt-4 font-display text-5xl leading-tight text-white sm:text-6xl">
          You, at scale.
        </h2>
        <p className="mt-4 max-w-md text-ink-400">The engine runs while you create. Set your first automation in minutes.</p>
        <div className="mt-8 flex items-center gap-4">
          <Link to={ROUTES.REGISTER} className="rounded-xl bg-brand-600 px-7 py-3.5 text-sm font-semibold text-white shadow-elevated transition-transform hover:-translate-y-0.5">
            Start free
          </Link>
          <Link to={ROUTES.LOGIN} className="rounded-xl border border-ink-700 px-7 py-3.5 text-sm font-medium text-ink-200 transition-colors hover:border-ink-500">
            Sign in
          </Link>
        </div>
      </section>
    </>
  );
};

/* ── Scene assembly ── */
const World = () => {
  const mats = useMats();
  return (
    <group>
      <SurfacePost mats={mats} />
      <Hall mats={mats} />

      {/* main rail: from beneath the comment down through the hall */}
      <Rail
        mats={mats}
        packets={7}
        points={[
          [0, -1.6, 0], [0, -6, -2], [1.5, -10, -6],
          [2.5, -13, -10], [0.5, -16, -13], [-2, -18, -16],
          [-1, -21, -19], [0, -23.4, -22],
        ]}
      />
      {/* secondary ambience rails */}
      <Rail mats={mats} packets={3} color="#312e81" points={[[-6, -9, -3], [-5, -14, -9], [-6, -19, -15], [-5, -24, -21]]} />
      <Rail mats={mats} packets={3} color="#312e81" points={[[6, -8, -4], [5, -13, -10], [6, -18, -16], [5, -23, -22]]} />

      <ScannerStation mats={mats} position={[2.5, -13, -10]} />
      <SortingGate mats={mats} position={[-2, -18, -16]} />
      <Assembler mats={mats} position={[0, -23, -22]} />
      <LaunchTube mats={mats} position={[0, -27, -28]} />

      {/* lighting */}
      <ambientLight intensity={0.12} />
      <directionalLight position={[6, 4, 8]} intensity={0.7} color="#c7d2fe" />
      <fog attach="fog" args={['#050508', 8, 42]} />
    </group>
  );
};

const EngineScene = () => (
  <Canvas
    dpr={[1, 1.8]}
    gl={{ antialias: true, powerPreference: 'high-performance' }}
    camera={{ fov: 55, near: 0.1, far: 80, position: [0, 0.4, 9] }}
  >
    <color attach="background" args={['#050508']} />
    <ScrollControls pages={7} damping={0.28}>
      <CameraRig />
      <World />
      <Scroll html style={{ width: '100%' }}>
        <Overlay />
      </Scroll>
    </ScrollControls>
    <Environment preset="city" />
    <EffectComposer>
      <Bloom intensity={0.9} luminanceThreshold={0.25} luminanceSmoothing={0.85} mipmapBlur />
      <Vignette eskil={false} offset={0.15} darkness={0.9} />
    </EffectComposer>
  </Canvas>
);

export default EngineScene;