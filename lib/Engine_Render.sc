// Engine_Render
// this engine is based entirely off of Eli Fieldsteel's
// beautifully succinct FM synth: https://sccode.org/1-5bA
Engine_Render : CroneEngine {
	// <Render>
	var renDiskBus;
	var renDiskSyn;
	var renDiskBuf;
	var renInput;
	// </Render>
	
	
	*new { arg context, doneCallback;
		^super.new(context, doneCallback);
	}
	
	alloc {
		
		// <Render>
		renDiskSyn=Array.newClear(2);
		renDiskBuf=Array.newClear(2);

		SynthDef("diskout", { arg bufnum=0, inbus=0;
			DiskOut.ar(bufnum,In.ar(inbus,1));
		}).add;
		
		// initialize synth defs
		SynthDef("renderInput", {
			arg ch, bus, diskout;
			var snd;

			snd=SoundIn.ar(ch)

			Out.ar(out, Pan2.ar(snd));
			Out.ar(diskout, snd);
		}).add;

		renDiskBus = Array.new(2, {arg i;
			Bus.audio(context.server,1);
		});
		context.server.sync;
		renInput = Array.new(2, {arg i;
			Synth("renderInput",[\ch,i,\out,0,\diskout,renDiskBus[i]])
		});
		
		this.addCommand("record_start","ss",{ arg msg;
			(0..1).do({arg voice;
			if (renDiskSyn[i]==nil,{
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

		this.addCommand("record_start","",{ arg msg;
			},{
				// don't record
				// if recording, free everything
				if (renDiskSyn.at(voice)!=nil,{
					("stopping recording for "++voice).postln;
					renDiskSyn.at(voice).free;
					renDiskSyn.removeAt(voice);
					renDiskBuf.at(voice).free;
					renDiskBuf.removeAt(voice);
				});
			});
			Synth.before(renSyn,"Render",[
				\diskout,renDiskBus.at(voice),
				\freq,msg[1].midicps,
				\amp,msg[2],
				\pan,msg[3],
				\atk,msg[4],
				\rel,msg[5],
				\cAtk,msg[6],
				\cRel,msg[7],
				\mRatio,msg[8],
				\cRatio,msg[9],
				\index,msg[10],
				\iScale,msg[11],
				\fxsend,msg[12],
				\eqFreq,msg[13],
				\eqDB,msg[14],
				\lpf,msg[15],
				\noise,msg[16],
				\natk,msg[17],
				\nrel,msg[18],
				\out,0,
				\fx,renBus,
			]).onFree({
				NetAddr("127.0.0.1",10111)
					.sendMsg("odashodasho_voice",voice++" "++msg[1],0);
			});
			// NodeWatcher.register(renVoices.at(fullname));
		});
		// </Render>
	}
	
	
	free {
		// <Render>
		renBus.free;
		renSyn.free;
		renVoices.keysValuesDo({ arg key, value; value.free; });
		renDiskBus.keysValuesDo({ arg key, value; value.free; });
		renDiskSyn.keysValuesDo({ arg key, value; value.free; });
		renDiskBuf.keysValuesDo({ arg key, value; value.free; });
		renSampleBuf.keysValuesDo({ arg key, value; value.free; });
		renSampleSyn.keysValuesDo({ arg key, value; value.free; });
		// </Render>
	}
}

