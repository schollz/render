-- shared render utilities


function setup_params()
  -- do not monitor
  audio.level_monitor(0)
  params:add_separator("recording")
  params:add{type="binary",name="record",id="record",behavior="toggle",action=function(x)
    if x==1 then
      record_start()
    else
      record_stop()
    end
  end
}
  for i=1,2 do
    params:add_separator("input "..i)
    params:add_control(i.."input_lpf","lpf",controlspec.new(20,20000,'exp',0,20000,'Hz'))
    params:set_action(i.."input_lpf",function(x)
      engine.input_lpf(i,x,params:get(i.."input_gain"))
    end)
    params:add_control(i.."input_gain","gain",controlspec.new(0.5,2,'lin',0,1,'Hz'))
    params:set_action(i.."input_gain",function(x)
      engine.input_lpf(i,params:get(i.."input_lpf"),x)
    end)
  end
end


function record_start()
  os.execute("mkdir -p ".._path.audio.."render")
  recording_filename=_path.audio.."render/"..os.date("%Y%m%d-%H%m%S")..".wav"
  print("recording "..recording_filename)
  engine.record_start(fname)
end

function record_stop()
  print("stopped recording "..recording_filename)
  engine.record_stop()
end

function lfo(period,dlo,dhi)
  local m=math.sin(2*math.pi*clock.get_beats()*clock.get_beat_sec()/period)
  return util.linlin(-1,1,dlo,dhi,m)
end
