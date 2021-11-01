-- shared render utilities

function record_start()
  engine.record_start()
end

function record_stop()
  engine.record_stop()
end

function lfo(period,dlo,dhi)
  local m=math.sin(2*math.pi*clock.get_beats()*clock.get_beat_sec()/period)
  return util.linlin(-1,1,dlo,dhi,m)
end
