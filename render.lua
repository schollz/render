-- render


function init()
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
    routing[6]="op-1" -- melody
    routing[8]="boutique" -- pad
    md["op-z"].conn.event=function(data)
        local d=midi.to_msg(data)
        if d.ch==nil then 
            do return end 
        end
        if routing[d.ch]==nil then 
            do return end 
        end
        if d.type=="note_on" then
            md[routing[d.ch]].conn:note_on(d.note,d.vel)
        elseif d.type=="note_off" then 
            md[routing[d.ch]].conn:note_off(d.note)
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
            for k,_ in pairs(cclfos) do
                for _,v in ipairs(cclfos[k]) do                    
                    md[k].conn:cc(v[1],math.floor(lfo(v[2],v[3],v[4])))
                end
            end
        end
    end)
    print("loaded")
end


function lfo(period,dlo,dhi)
    local m=math.sin(2*math.pi*clock.get_beats()*clock.get_beat_sec()/period)
    return util.linlin(-1,1,dlo,dhi,m)
end
