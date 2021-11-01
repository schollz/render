// Engine_Render
// this engine is based entirely off of Eli Fieldsteel's
// beautifully succinct FM synth: https://sccode.org/1-5bA
Engine_Render : CroneEngine {
	// <Render>
	var renDiskBus;
	var renDiskSyn;
	var renDiskBuf;
	var renInput;
	var renBass;
	// </Render>
	
	
	*new { arg context, doneCallback;
		^super.new(context, doneCallback);
	}
	
	alloc {
		
		// <Render>
		renDiskSyn=Array.newClear(2);
		renDiskBuf=Array.newClear(2);
		renInput=Array.newClear(2);

		SynthDef("diskout", { arg bufnum=0, inbus=0;
			DiskOut.ar(bufnum,In.ar(inbus,1));
		}).add;
		
		// initialize synth defs
		SynthDef("renderInput", {
			arg ch, bus, diskout, lpf=20000, gain=1;
			var snd;

			snd=SoundIn.ar(ch);
			snd=MoogFF.ar(snd,lpf,gain);

			Out.ar(out, Pan2.ar(snd));
			Out.ar(diskout, snd);
		}).add;

		renDiskBus = Array.new(2, {arg i;
			Bus.audio(context.server,1);
		});
		context.server.sync;
	

		SynthDef("bass", {
                        arg out,diskout,note=60,amp=0,attack=0.01,decay=0.1,sustain=0.9,release=1,
                        t_trig=1,lpf=2;
                        var snd,env;
                        env=EnvGen.ar(Env.adsr(attack,decay,sustain,release),gate:t_trig,doneAction:2);
                        amp=Lag.kr(amp,2);
                        lpf=Lag.kr(lpf,2);
                        snd=Pulse.ar(note.midicps,width:SinOsc.kr(1/3).range(0.2,0.4));
                        snd=snd+LPF.ar(WhiteNoise.ar(SinOsc.kr(1/rrand(3,4)).range(1,rrand(3,4))),2*note.midicps);
                        snd = HPF.ar(snd,60);
                        snd = LPF.ar(snd,lpf*note.midicps);
                        snd = snd*(60/note.midicps);
			snd = snd.tanh*amp*env;                        
			Out.ar(out, Pan2.ar(snd));
			Out.ar(diskout, snd);
                }).add;

		this.addCommand("input","i",{ arg msg;
			var i=msg[1];
			renInput[i]=Synth.head(nil,"renderInput",[\ch,i,\out,0,\diskout,renDiskBus[i]]);
		});

		this.addCommand("input_lpf","iff",{ arg msg;
			var i=msg[1];
			if (renInput[i]!=nil,{
				renInput[i].set(\lpf,msg[2],\gain,msg[3]);
			});
		});

		this.addCommand("bass","ffffffi",{arg msg;
			if (renBass.isNil,{
				renBass=Synth.head(nil,"bass",[\out,0,\diskout,renDiskBus[1]]);
			})
			renBass.set(\t_trig,0);
			if (msg[7]>0,{
			renBass.set(\t_trig,1,
				\note,msg[1],
				\amp,msg[2],
				\attack,msg[3],
				\decay,msg[4],
				\sustain,msg[5],
				\release,msg[6]);
			});
					
		});
	
		this.addCommand("record_stop","",{ arg msg;
			(0..1).do({arg voice;
			if (renDiskSyn[voice]!=nil,{
				renDiskSyn[voice].free;
				renDiskSyn[voice].free;
			});
			});
		});

		this.addCommand("record_start","ss",{ arg msg;
			(0..1).do({arg voice;
			if (renDiskSyn[voice]==nil,{
				var b=Buffer.alloc(context.server,65536,1);
				var pathname=msg[i].asString;
				("allocating buffer for to "++pathname).postln;
				b.write(pathname.standardizePath,PathName.new(pathname.standardizePath).extension,"int16",0,0,true);
				renDiskBuf[voice]=b;
				renDiskSyn[voice]=Synth.tail(nil,"diskout",
					[\bufnum,renDiskBuf[voice],\inbus,renDiskBus[voice]]				);
				// initiate disk syn
			});
			});
		});

		// </Render>
	}
	
	
	free {
		// <Render>
		renDiskBus.do({ arg value,i; value.free; });
		renDiskSyn.do({ arg value,i; value.free; });
		renDiskBuf.do({ arg value,i; value.free; });
		renInput.do({ arg value,i; value.free; });
		// </Render>
	}
}

