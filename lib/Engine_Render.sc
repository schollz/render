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

