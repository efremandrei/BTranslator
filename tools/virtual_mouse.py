"""Emulator-only test source; creates an actual Android uinput mouse. No app/root changes.

Run while connectedDebugAndroidTest executes. Stop with Ctrl+C or create work/stop-mouse.
The APK itself never uses uinput, ADB, or shell privileges.
"""
import json, os, subprocess, time
from pathlib import Path

adb=os.environ.get('ADB',r'C:\Users\andre\AppData\Local\Android\Sdk\platform-tools\adb.exe')
work=Path('work');work.mkdir(exist_ok=True)
stop=work/'stop-mouse'
if stop.exists():stop.unlink()
with (work/'virtual-mouse.log').open('w',encoding='utf-8') as log:
    proc=subprocess.Popen([adb,'shell','uinput','-'],stdin=subprocess.PIPE,stdout=log,stderr=subprocess.STDOUT,text=True)
    def send(command):
        proc.stdin.write(json.dumps({'id':1,**command})+'\n');proc.stdin.flush()
    try:
        send({'command':'register','name':'BTranslator QA Mouse','vid':0x1234,'pid':0x5678,'bus':'usb','port':'btranslator-qa','configuration':[{'type':100,'data':[1,2]},{'type':101,'data':[272,273]},{'type':102,'data':[0,1,6,8]}]})
        time.sleep(2)
        print('Virtual mouse registered; alternating left/right input bursts.',flush=True)
        direction=-1
        while not stop.exists():
            if proc.poll() is not None:raise RuntimeError('uinput exited; see work/virtual-mouse.log')
            send({'command':'updateTimeBase'})
            for _ in range(4):
                send({'command':'inject','events':[2,0,28*direction,0,0,0]})
                send({'command':'delay','duration':25})
            direction=-direction
            time.sleep(1.25)
    finally:
        proc.stdin.close()
        try:proc.wait(timeout=5)
        except subprocess.TimeoutExpired:proc.terminate()
        print('Virtual mouse removed.',flush=True)
