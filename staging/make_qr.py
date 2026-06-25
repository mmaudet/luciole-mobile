# staging/make_qr.py — generate the two onboarding QR codes.
# Usage: python make_qr.py --ssid Luciole --pass motdepasse --ip 192.168.x.1
import argparse, qrcode

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--ssid", required=True)
    ap.add_argument("--pass", dest="pwd", required=True)
    ap.add_argument("--ip", required=True)
    ap.add_argument("--port", default="8080")
    a = ap.parse_args()
    # WIFI join payload (WPA). Auto-joins on scan.
    qrcode.make(f"WIFI:T:WPA;S:{a.ssid};P:{a.pwd};;").save("staging/wifi-join.png")
    qrcode.make(f"http://{a.ip}:{a.port}/").save("staging/open-url.png")
    print("wrote staging/wifi-join.png and staging/open-url.png")

if __name__ == "__main__":
    main()
