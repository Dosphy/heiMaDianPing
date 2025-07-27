local voucherId = ARGV[1]
local userId = ARGV[2]

local stockKey = 'seckill:stock:' .. voucherId
local stockKey = 'seckill:order:' .. voucherId

if(tonumber(redis.call('get',stockKey)) <= 0) then
    return 1
end