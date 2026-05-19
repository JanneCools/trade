-- datatypes
@page_id:integer
@page_title:string
@value_valid_from:datetime_in_seconds
@type:string
@votes_for_election:long
@needed_votes:long
@turnout:decimal_scale_2
@electoral_vote1:long
@popular_vote1:long
@percentage1:decimal_scale_1
@electoral_vote2:long
@popular_vote2:long
@percentage2:decimal_scale_1

-- univariate
type in ('presidential', 'parliamentary', 'legislative', 'primary', 'by-election')
votes_for_election >= 1
votes_for_election <= 1000
needed_votes >= 1
needed_votes <= 1000
turnout >= 0
turnout <= 100
electoral_vote1 >= 1
electoral_vote1 <= 1000
popular_vote1 >= 1
popular_vote1 <= 2000000000
percentage1 >= 0
percentage1 <= 100
electoral_vote2 >= 1
electoral_vote2 <= 1000
popular_vote2 >= 1
popular_vote2 <= 2000000000
percentage2 >= 0
percentage2 <= 100

-- multivariate: votes
NOT needed_votes > votes_for_election
NOT votes_for_election >= 100 & needed_votes <= 50
NOT votes_for_election >= 200 & needed_votes <= 100
NOT votes_for_election >= 300 & needed_votes <= 150
NOT votes_for_election >= 400 & needed_votes <= 200
NOT votes_for_election >= 500 & needed_votes <= 250

-- multivariate: parties
NOT electoral_vote1 > votes_for_election
NOT electoral_vote2 > votes_for_election
NOT electoral_vote1 < needed_votes

NOT popular_vote1 > popular_vote2 & percentage1 <= percentage2
NOT popular_vote2 > popular_vote1 & percentage2 <= percentage1
NOT percentage1 >= 50 & percentage2 > 50
NOT percentage1 >= 60 & percentage2 > 40

-- transition: attributes should not change value
NOT page_title#next != page_title#curr
NOT type#next != type#curr
NOT votes_for_election#next != votes_for_election#curr
NOT needed_votes#next != needed_votes#curr
NOT turnout#next > S^130(turnout#curr)
NOT turnout#next < S^-130(turnout#curr)

NOT electoral_vote1#next != electoral_vote1#curr
NOT popular_vote1#curr <= 1000000 & popular_vote1#next > S^50000(popular_vote1#curr)
NOT popular_vote1#curr <= 1000000 & popular_vote1#next < S^-50000(popular_vote1#curr)
NOT popular_vote1#next > S^300000(popular_vote1#curr)
NOT popular_vote1#next < S^-300000(popular_vote1#curr)
NOT percentage1#next > S^13(percentage1#curr)
NOT percentage1#next < S^-13(percentage1#curr)

NOT electoral_vote2#next != electoral_vote2#curr
NOT popular_vote2#curr <= 1000000 & popular_vote2#next > S^50000(popular_vote2#curr)
NOT popular_vote2#curr <= 1000000 & popular_vote2#next < S^-50000(popular_vote2#curr)
NOT popular_vote2#next > S^300000(popular_vote2#curr)
NOT popular_vote2#next < S^-300000(popular_vote2#curr)
NOT percentage2#next > S^13(percentage2#curr)
NOT percentage2#next < S^-13(percentage2#curr)
