-- datatypes
@line_nr:integer
@player:string
@year:integer
@age:integer
@yrs_experience:integer
@team:string
@position:string
@games:integer
@minutes_played:integer
@vorp:decimal_scale_2
@salary:integer
@drb:decimal_scale_2
@orb:decimal_scale_2
@trb:decimal_scale_2


-- univariate rules
year > 1977
age > 15
age < 60
yrs_experience >= 0
yrs_experience < 45
team in {'ATL', 'BOS', 'BRK', 'BUF', 'CHA', 'CHH', 'CHI', 'CHO', 'CLE', 'DAL', 'DEN', 'DET', 'GSW', 'HOU', 'IND', 'KCK', 'LAC', 'LAL', 'MEM', 'MIA', 'MIL', 'MIN', 'NJN', 'NOH', 'NOJ', 'NOK', 'NOP', 'NYK', 'OKC', 'ORL', 'PHI', 'PHO', 'POR', 'SAC', 'SAS', 'SDC', 'SEA', 'TOR', 'UTA', 'VAN', 'WAS', 'WSB'}
position in {'PG', 'SG', 'SF', 'PF', 'C'}
games > 0
games < 83
minutes_played > 0
minutes_played < 3936
vorp >= -3
vorp <= 15
salary >= 0
salary < 62000000
drb >= 0
drb <= 45
orb >= 0
orb <= 40
trb >= 0
trb <= 45

-- multivariate rules
NOT year == 1999 & games > 50
NOT year == 2012 & games > 66

NOT year == 1999 & minutes_played > 2400
NOT year == 2012 & minutes_played > 3168

NOT vorp < -0.48 & salary > 1000000

NOT position == 'PG' & drb > 20
NOT position == 'SG' & drb > 25
NOT position == 'SF' & drb > 30
NOT position == 'PF' & drb > 35

NOT trb < drb & trb < orb
NOT trb > drb & trb > orb

-- multivariate rules

-- transition rules
NOT age#next < age#curr
NOT year#next == year#curr & age#next != age#curr
NOT year#next == S^1(year#curr) & age#next != S^1(age#curr)

NOT yrs_experience#next < yrs_experience#curr
NOT year#next == year#curr & yrs_experience#next != yrs_experience#curr
NOT year#next == S^1(year#curr) & yrs_experience#next != S^1(yrs_experience#curr)

NOT year#next == year#curr & team#next == team#curr

NOT position#curr == 'PG' & position#next == 'PF'
NOT position#curr == 'PG' & position#next == 'C'
NOT position#curr == 'SG' & position#next == 'PF'
NOT position#curr == 'SG' & position#next == 'C'
NOT position#curr == 'SF' & position#next == 'PG'
NOT position#curr == 'PF' & position#next == 'PG'
NOT position#curr == 'C' & position#next == 'PG'
NOT position#curr == 'PF' & position#next == 'SG'

NOT vorp#next > -0.48 & vorp#curr > -0.48 & vorp#next == vorp#curr & salary#next != salary#curr
NOT vorp#next > -0.48 & vorp#curr > -0.48 & vorp#next < vorp#curr & salary#next >= salary#curr
NOT vorp#next > -0.48 & vorp#curr > -0.48 & vorp#next > vorp#curr & salary#next <= salary#curr