-- render

include("render/lib/render")

debounce_crow=0

function init()
  setup_params()
  connect_midi()

  params:add_separator("crow")
  filter_freq=controlspec.new(20,20000,'exp',0,20000,'Hz')
  params:add_control("crow_attack","attack",controlspec.new(0.005,10,'lin',0.005,0.01,'s',0.005/10))
  params:add_control("crow_decay","decay",controlspec.new(0.005,10,'lin',0.005,0.01,'s',0.005/10))
  params:add_control("crow_sustain","sustain",controlspec.new(0.005,10,'lin',0.005,10,'volts',0.005/10))
  params:add_control("crow_release","release",controlspec.new(0.005,10,'lin',0.005,0.01,'s',0.005/10))
  for _,v in ipairs({"attack","decay","sustain","release"}) do
    params:set_action("crow_"..v,function(x) debounce_crow=5 end)
  end
  params:add_separator("bass")
  filter_freq=controlspec.new(20,20000,'exp',0,20000,'Hz')
  params:add_control("bass_attack","attack",controlspec.new(0.005,10,'lin',0.005,0.01,'s',0.005/10))
  params:add_control("bass_decay","decay",controlspec.new(0.005,10,'lin',0.005,0.01,'s',0.005/10))
  params:add_control("bass_sustain","sustain",controlspec.new(0.005,1,'lin',0.005,1,'amp',0.005/1))
  params:add_control("bass_release","release",controlspec.new(0.005,10,'lin',0.005,0.01,'s',0.005/10))
end

local bass_velocity={1,3,5,8,10,12,14,17,19,21,23,26,28,30,32,34,37,39,41,43,45,47,49,51,53,55,57,59,61,63,65,67,68,70,72,74,75,77,79,80,82,83,85,86,88,89,91,92,93,95,96,97,98,99,100,101,102,103,104,105,106,107,108,109,110,111,111,112,113,114,114,115,116,116,117,117,118,118,119,119,120,120,121,121,121,122,122,122,123,123,123,124,124,124,124,125,125,125,125,125,125,126,126,126,126,126,126,126,126,126,126,126,126,127,127,127,127,127,127,127,127,127,127,127,127,127,127,127}
function play_bass(on,note,velocity)
  engine.bass(note,bass_velocity[velocity+1]/127,
    params:get("bass_attack"),
    params:get("bass_decay"),
    params:get("bass_sustain"),
  params:get("bass_release"),on and 1 or 0)
end

function play_crow(on,note,velocity)
  crow.output[1]=(note-21)/12
  crow.output[2](on)
end

function update_crow()
  crow.output[2].action=adsr(
    params:get("crow_attack"),
    params:get("crow_decay"),
    params:get("crow_sustain"),
    params:get("crow_release"),
  )
end

function play_crow(on,note,velocity)

end

function connect_midi()
  md={}
  for _,dev in pairs(midi.devices) do
    if dev.port~=nil then
      local name=string.lower(dev.name)
      print(name)
      md[name]={
        name=name,
        port=dev.port,
        conn=midi.connect(dev.port),
      }
    end
  end
  routing={}
  -- bass
  routing[5]=function(on,note,vel)
    play_bass(on,note,vel)
  end
  -- melody
  routing[6]=function(on,note,vel)
    if on then
      md["op-1"].conn:note_on(note,vel)
    else
      md["op-1"].conn:note_off(note)
    end
  end
  -- arp
  routing[7]=function(on,note,vel)
    play_crow(on,note,vel)
  end
  -- pad
  routing[8]=function(on,note,vel)
    if on then
      md["boutique"].conn:note_on(note,vel)
    else
      md["boutique"].conn:note_off(note)
    end
  end
  md["op-z"].conn.event=function(data)
    local d=midi.to_msg(data)
    if d.ch==nil then
      do return end
    end
    if routing[d.ch]==nil then
      do return end
    end
    if d.type=="note_on" then
      routing[d.ch](true,d.note,d.vel)
    elseif d.type=="note_off" then
      routing[d.ch](false,d.note)
    end
  end

  -- ccs
  cclfos={}
  cclfos["boutique"]={}
  cclfos["op-1"]={}
  -- cc lfo: cc #, period , min val, max val
  table.insert(cclfos["boutique"],{15,10,1,120}) -- VCO PW
  table.insert(cclfos["boutique"],{19,11.5,1,120}) -- VCO PWM level
  table.insert(cclfos["boutique"],{21,13.5,1,120}) -- VCO sub level
  table.insert(cclfos["boutique"],{74,21,80,110}) -- VCO sub level
  table.insert(cclfos["op-1"],{0
  ,13.5,1,120}) -- VCO sub level
  --table.insert(cclfos["op-1"],{4,13.5,1,120}) -- VCO sub level
  clock.run(function()
    while true do
      clock.sleep(0.1)
      if debounce_crow>0 then
        debounce_crow=debounce_crow-1
        if debounce_crow==1 then
          update_crow()
        end
      end

      for k,_ in pairs(cclfos) do
        for _,v in ipairs(cclfos[k]) do
          md[k].conn:cc(v[1],math.floor(lfo(v[2],v[3],v[4])))
        end
      end
    end
  end)
  print("loaded")
end

