# mTLS Example

This example demonstrates a mock server with mutual TLS (mTLS) authentication.

## Files

- `server-config-mtls-example.jsonmc` - Server configuration with embedded TLS certificates
- `start_server.sh` - Script to start the server
- `start_client.sh` - Script to test the server with client certificates
- `ca-cert.pem` - Certificate Authority certificate
- `ca-key.pem` - Certificate Authority private key
- `client-cert.pem` - Client certificate (signed by CA)
- `client-key.pem` - Client private key
- `client-csr.pem` - Client certificate signing request (intermediate file)

## Usage

1. Start the server:
   ```bash
   ./start_server.sh
   ```

2. In another terminal, run the client tests:
   ```bash
   ./start_client.sh
   ```

## Certificate Details

The certificates in this directory are self-signed and generated for demonstration purposes only. They should not be used in production.

- **CA Certificate**: Used to sign and verify client certificates
- **Client Certificate**: Presented by the client to authenticate with the server
- **Server Certificate**: Embedded in the configuration file

## How mTLS Works

1. Client connects to the server
2. Server presents its certificate
3. Client verifies server certificate against trusted CA
4. Client presents its own certificate
5. Server verifies client certificate against its trusted CA
6. Encrypted connection is established with mutual authentication
