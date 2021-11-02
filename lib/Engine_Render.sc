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
		renInput=Array.newClear(2);
		renDiskBus = Bus.audio(context.server,2);

		SynthDef("diskout", { arg bufnum=0, inbus=0;
			DiskOut.ar(bufnum,In.ar(inbus,2));
		}).add;
		
		// initialize synth defs
		SynthDef("renderInput", {
			arg ch, out, bus, diskout, lpf=20000, gain=1;
			var snd;

			snd=SoundIn.ar(ch);
			snd=MoogFF.ar(snd,lpf,gain);

			Out.ar(out, Pan2.ar(snd));
			Out.ar(diskout, Pan2.ar(snd,2*ch-1));
		}).add;

		context.server.sync;

		SynthDef("bass", {
			arg out,diskout,ch=1,note=60,amp=0,attack=0.01,decay=0.1,sustain=0.9,release=1,
			gate=1,lpf=2;
			var snd,env;
			env=EnvGen.ar(Env.adsr(attack,decay,sustain,release),gate:gate,doneAction:2);
			snd=Pulse.ar(note.midicps,width:SinOsc.kr(1/3).range(0.2,0.4));
			snd=snd+LPF.ar(WhiteNoise.ar(SinOsc.kr(1/rrand(3,4)).range(1,rrand(3,4))),2*note.midicps);
			snd = HPF.ar(snd,60);
			snd = LPF.ar(snd,lpf*note.midicps);
			snd = snd*(60/note.midicps);
			snd = snd.tanh*amp*env;                        
			Out.ar(out, Pan2.ar(snd));
			Out.ar(diskout, Pan2.ar(snd,2*ch-1));
        }).add;

		this.addCommand("input","i",{ arg msg;
			var i=msg[1];
			renInput[i]=Synth.head(nil,"renderInput",[\ch,i,\out,0,\diskout,renDiskBus]);
		});

		this.addCommand("input_lpf","iff",{ arg msg;
			var i=msg[1];
			if (renInput[i]!=nil,{
				renInput[i].set(\lpf,msg[2],\gain,msg[3]);
			});
		});

		this.addCommand("bass","ffffffi",{arg msg;
			if (renBass.isNil,{
				renBass=Synth.head(nil,"bass",[\out,0,\diskout,renDiskBus,\amp,0]);
				NodeWatcher.register(renBass);
			},{
				if (renBass.isRunning==false,{
					renBass=Synth.head(nil,"bass",[\out,0,\diskout,renDiskBus,\amp,0]);
					NodeWatcher.register(renBass);
				});
			});
			renBass.set(\gate,0);
			if (msg[7]>0,{
				renBass.set(\gate,1,
					\note,msg[1],
					\amp,msg[2],
					\attack,msg[3],
					\decay,msg[4],
					\sustain,msg[5],
					\release,msg[6]);
			});
		});
	
		this.addCommand("record_stop","",{ arg msg;
			if (renDiskSyn!=nil,{
				renDiskSyn.free;
				renDiskBuf.free;
			});
		});

		this.addCommand("record_start","s",{ arg msg;
			if (renDiskSyn==nil,{
				var b=Buffer.alloc(context.server,65536,2);
				var pathname=msg[1].asString;
				("allocating buffer for to "++pathname).postln;
				b.write(pathname.standardizePath,
					PathName.new(pathname.standardizePath).extension,"int16",0,0,true);
				renDiskBuf=b;
				renDiskSyn=Synth.tail(nil,"diskout",
					[\bufnum,renDiskBuf,\inbus,renDiskBus]);
				// initiate disk syn
			});
		});

		// </Render>
	}
	
	
	free {
		// <Render>
		renDiskBus.free;
		renDiskSyn.free;
		renDiskBuf.free;
		renInput.do({ arg value,i; value.free; });
		// </Render>
	}
}

