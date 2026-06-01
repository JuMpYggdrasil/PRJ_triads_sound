import io, math, tkinter as tk, wave
from array import array
import pygame

TRIADS = [
    ("C",  [("C",48),("Eb",51),("E",52),("G",55)]),
    ("C#", [("C#",49),("E",52),("F",53),("G#",56)]),
    ("D",  [("D",50),("F",53),("F#",54),("A",57)]),
    ("Eb", [("Eb",51),("Gb",54),("G",55),("Bb",58)]),
    ("E",  [("E",52),("G",55),("G#",56),("B",59)]),
    ("F",  [("F",53),("Ab",56),("A",57),("C",60)]),
    ("F#", [("F#",54),("A",57),("A#",58),("C#",61)]),
    ("G",  [("G",55),("Bb",58),("B",59),("D",62)]),
    ("Ab", [("Ab",56),("B",59),("C",60),("Eb",63)]),
    ("A",  [("A",57),("C",60),("C#",61),("E",64)]),
    ("Bb", [("Bb",58),("Db",61),("D",62),("F",65)]),
    ("B",  [("B",59),("D",62),("D#",63),("F#",66)]),
]

COLS = ["Root","3b","3","5th"]
COLORS = {"Root":"#42A5F5","3b":"#AB47BC","3":"#66BB6A","5th":"#FFA726"}

class PianoSynth:
    def __init__(self):
        pygame.mixer.init(frequency=44100, size=-16, channels=1, buffer=512)
        self.sounds = {}
        self.active = {}
        seen = set()
        for _, btns in TRIADS:
            for _, m in btns:
                if m not in seen:
                    seen.add(m)
                    self.sounds[m] = pygame.mixer.Sound(buffer=self._wav(m))

    @staticmethod
    def _wav(midi):
        freq, sr, dur = 440.0 * 2.0**((midi-69)/12.0), 44100, 3.0
        n = int(sr * dur)
        s = array("h", [0]) * n
        for i in range(n):
            t = i / sr
            env = t/0.008 if t < 0.008 else (
                1.0-0.25*(t-0.008)/0.052 if t < 0.06 else (
                0.75-0.25*(t-0.06)/0.44 if t < 0.5 else
                0.5*math.exp(-1.2*(t-0.5))))
            if env <= 0.001: continue
            v = sum((1/h)*math.exp(-2.5*(h-1)*t)*math.sin(2*math.pi*freq*h*t) for h in range(1,9))
            s[i] = int(v * env * 0.4 * 32767)
        buf = io.BytesIO()
        with wave.open(buf,"wb") as w:
            w.setnchannels(1); w.setsampwidth(2); w.setframerate(sr)
            w.writeframes(s.tobytes())
        return buf.getvalue()

    def note_on(self, m):
        if m in self.sounds:
            if m in self.active:
                try: self.active[m].stop()
                except: pass
            self.active[m] = self.sounds[m].play()

    def note_off(self, m):
        ch = self.active.pop(m, None)
        if ch:
            try: ch.fadeout(60)
            except: pass

    def quit(self):
        for m in list(self.active): self.note_off(m)
        try: pygame.mixer.quit()
        except: pass

class TriadsApp:
    def __init__(self, synth):
        self.synth = synth
        self.root = tk.Tk()
        self.root.title("Triads Sound")
        self.root.configure(bg="#0A0A1A")
        self.root.minsize(480, 320)
        self._build()

    def _build(self):
        hdr = tk.Frame(self.root, bg="#1A1A2E", height=36)
        hdr.pack(fill=tk.X)
        hdr.pack_propagate(False)
        for i, col in enumerate(COLS):
            c = COLORS[col]
            tk.Label(hdr, text=col, fg=c, bg="#1A1A2E",
                     font=("Segoe UI",10,"bold"), anchor=tk.CENTER
            ).grid(row=0, column=i, sticky="nsew", padx=2, pady=6)
            hdr.grid_columnconfigure(i, weight=1, uniform="x")

        cv = tk.Canvas(self.root, bg="#0A0A1A", highlightthickness=0)
        sb = tk.Scrollbar(self.root, orient=tk.VERTICAL, command=cv.yview)
        cv.configure(yscrollcommand=sb.set)
        sb.pack(side=tk.RIGHT, fill=tk.Y)
        cv.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        inner = tk.Frame(cv, bg="#0A0A1A")
        wid = cv.create_window((0,0), window=inner, anchor=tk.NW)
        inner.bind("<Configure>", lambda _: cv.configure(scrollregion=cv.bbox("all")))
        cv.bind("<Configure>", lambda e: cv.itemconfig(wid, width=e.width))
        cv.bind_all("<MouseWheel>", lambda e: cv.yview_scroll(-(e.delta//120),"units"))

        for idx, (_, btns) in enumerate(TRIADS):
            bg = "#16213E" if idx%2==0 else "#0F3460"
            row = tk.Frame(inner, bg=bg)
            row.pack(fill=tk.X, padx=4, pady=2)
            for col in range(4): row.grid_columnconfigure(col, weight=1, uniform="x")
            for bi, (n, m) in enumerate(btns):
                clr = COLORS[COLS[bi]]
                r, g, b = int(clr[1:3],16), int(clr[3:5],16), int(clr[5:7],16)
                pbg = f"#{min(255,r+int((255-r)*.35)):02x}{min(255,g+int((255-g)*.35)):02x}{min(255,b+int((255-b)*.35)):02x}"
                btn = tk.Button(row, text=n, bg=clr, fg="#EEE",
                    activebackground=pbg, activeforeground="#EEE",
                    relief=tk.RAISED, bd=1, font=("Segoe UI",13,"bold"),
                    cursor="hand2")
                btn.bind("<ButtonPress-1>", lambda _,m=m: self.synth.note_on(m))
                btn.bind("<ButtonRelease-1>", lambda _,m=m: self.synth.note_off(m))
                btn.grid(row=0, column=bi, sticky="nsew", padx=1, pady=2)

    def run(self):
        try: self.root.mainloop()
        finally: self.synth.quit()

if __name__ == "__main__":
    try:
        TriadsApp(PianoSynth()).run()
    except RuntimeError as e:
        r = tk.Tk()
        r.title("Triads Sound")
        tk.Label(r, text=str(e), fg="red", bg="#0A0A1A",
                 font=("Segoe UI",11)).pack(padx=20, pady=20)
        r.mainloop()
