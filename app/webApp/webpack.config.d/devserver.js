// Bind the dev server to all interfaces so it's reachable on the local network.
// Access via http://<your-machine-ip>:<port>
if (config.devServer) {
    config.devServer.host = '0.0.0.0';
}
