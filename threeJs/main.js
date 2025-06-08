import * as THREE from "three";

const scene = new THREE.Scene();
const camera = new THREE.PerspectiveCamera(75, window.innerWidth / window.innerHeight, 0.1, 1000);
const renderer = new THREE.WebGLRenderer();
renderer.setSize(window.innerWidth, window.innerHeight);
document.body.appendChild(renderer.domElement);

const geometry = new THREE.BoxGeometry();
const material = new THREE.MeshNormalMaterial();
const cube = new THREE.Mesh(geometry, material);
scene.add(cube);
window.cube = cube;

camera.position.z = 5;

function animate() {
    requestAnimationFrame(animate);
    cube.rotation.x += 0.01;
    cube.rotation.y += 0.01;
    renderer.render(scene, camera);

    const { x, y, z, w } = cube.quaternion;
    const quaternionString = `Quaternion: (x: ${x.toFixed(2)}, y: ${y.toFixed(2)}, z: ${z.toFixed(2)}, w: ${w.toFixed(2)})`;
    window.kmpJsBridge.callNative("Quaternion", quaternionString, null);
}

animate();