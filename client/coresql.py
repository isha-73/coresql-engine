import socket
import struct
import sys

HOST = "127.0.0.1"
PORT = 5455

def send_query(sock, sql):
    payload = sql.encode("utf-8")
    header = struct.pack(">BI", 0x01, len(payload))
    sock.sendall(header + payload)

def recv_packet(sock):
    raw = sock.recv(5)
    if len(raw) < 5:
        return None, ""
    ptype, length = struct.unpack(">BI", raw)
    payload = b""
    while len(payload) < length:
        chunk = sock.recv(min(4096, length - len(payload)))
        if not chunk:
            break
        payload += chunk
    return ptype, payload.decode("utf-8")

def main():
    print(f"Connecting to CoreSQL at {HOST}:{PORT}")
    try:
        with socket.create_connection((HOST, PORT)) as sock:
            print("Connected! Type your SQL queries. Type 'EXIT' to quit.")
            while True:
                try:
                    sql = input("CoreSQL-Py> ")
                    if sql.strip().upper() == 'EXIT':
                        break
                    if not sql.strip():
                        continue
                        
                    send_query(sock, sql)
                    ptype, data = recv_packet(sock)
                    
                    if ptype == 0x03:
                        print(data)
                    elif ptype == 0x04:
                        print(f"Error: {data}")
                    else:
                        print(f"Unknown packet type {ptype}: {data}")
                        
                except EOFError:
                    break
    except ConnectionRefusedError:
        print("Connection refused. Make sure CoreSQL is running with --server")

if __name__ == "__main__":
    main()
