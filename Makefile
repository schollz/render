
format: lua-format.py
	python3 lua-format.py lib/render.lua
	python3 lua-format.py left.lua
	python3 lua-format.py right.lua

lua-format.py:
	wget https://raw.githubusercontent.com/schollz/LuaFormat/master/lua-format.py
