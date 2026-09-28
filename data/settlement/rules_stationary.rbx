-- datatypes
@page_id:integer
@page_title:string
@value_valid_from:datetime_in_seconds
@population_total:long
@population_density_km2:long
@area_total_km2:decimal_scale_2

-- univariate
population_total >= 0
population_total <= 2000000000
population_density_km2 >= 0
population_density_km2 <= 50000
area_total_km2 >= 0
area_total_km2 <= 1000000

-- multivariate
NOT area_total_km2 == 1 & population_density_km2 != population_total
NOT area_total_km2 > 1 & population_density_km2 >= population_total
NOT area_total_km2 < 1 & population_density_km2 <= population_total


