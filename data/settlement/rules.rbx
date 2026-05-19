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

-- transition
NOT area_total_km2#curr <= 100 & area_total_km2#next > S^1000(area_total_km2#curr)
NOT area_total_km2#curr <= 100 & area_total_km2#next < S^-1000(area_total_km2#curr)
NOT area_total_km2#next > S^3000(area_total_km2#curr)
NOT area_total_km2#next < S^-3000(area_total_km2#curr)
NOT population_total#curr <= 750000  & population_total#next > S^25000(population_total#curr)
NOT population_total#curr <= 750000 & population_total#next < S^-25000(population_total#curr)
NOT population_total#curr <= 1500000  & population_total#next > S^50000(population_total#curr)
NOT population_total#curr <= 1500000 & population_total#next < S^-50000(population_total#curr)
NOT population_total#curr <= 2500000  & population_total#next > S^100000(population_total#curr)
NOT population_total#curr <= 2500000 & population_total#next < S^-100000(population_total#curr)
NOT population_total#curr <= 3500000  & population_total#next > S^150000(population_total#curr)
NOT population_total#curr <= 3500000 & population_total#next < S^-150000(population_total#curr)
NOT population_total#next > S^200000(population_total#curr)
NOT population_total#next < S^-200000(population_total#curr)

NOT population_total#next > population_total#curr & population_density_km2#next < population_density_km2#curr
NOT population_total#next < population_total#curr & population_density_km2#next > population_density_km2#curr

