-- datatypes
@patient_upn:integer
@datum:date_in_days
@gender:string
@gender_txt:string
@test_indication:integer
@test_indication_txt:string
@living_status_i:integer
@living_status_i_txt:string
@living_status_ii:integer
@living_status_ii_txt:string
@previous_fall_s:integer
@previous_fall_s_txt:string
@previous_fall_12_months:integer
@previous_fall_12_months_txt:string
@fear_of_falling:integer
@fear_of_falling_txt:string
@physical_activities_no_yes:integer
@physical_activities_no_yes_txt:string
@mmse:integer
@eyeglasses_yes_no:integer
@eyeglasses_yes_no_txt:string
@eyeglasses_worn_during_test:integer
@eyeglasses_worn_during_test_txt:string
@eyeglasses_test_type_txt:string
@hearing_aid_no_yes:integer
@hearing_aid_no_yes_txt:string
@hearing_aid_side_txt:string
@hearing_aid_worn_during_test:integer
@hearing_aid_worn_during_test_txt:string
@hearing_aid_which_worn_during_test_txt:string
@height_m:decimal_scale_2
@weight_kg:decimal_scale_2
@bmi_kg_m2:decimal_scale_2
@leg_right:decimal_scale_1
@leg_left:decimal_scale_1

-- univariate
gender in {'f','F','m','M'}
gender_txt in {'Female', 'Male'}
test_indication in {1,2,3,4,5,6,7,999}
test_indication_txt in {'Gait Analysis', 'Inpatient', 'Memory Clinic routine', 'Mobility Diagnostics (MOBI)', 'Outpatient', 'Study', 'Suspected NPH', 'unknown'}
living_status_i in {1,2,3,4,5,999}
living_status_i_txt in {'apartment', 'house', 'other (inkl. Convent, assisted living)', 'senior residence', 'skilled nursing facility, nursing home', 'unknown'}
living_status_ii in {1,2,3,4,5,6,7,999}
living_status_ii_txt in {'alone', 'multiple', 'other', 'roommates (WG)', 'unknown', 'with child/ren', 'with partner', 'with relatives'}
previous_fall_s in {1,2,999}
previous_fall_s_txt in {'no', 'unknown or N/A', 'yes'}
previous_fall_12_months in {1,2,999}
previous_fall_12_months_txt in {'no', 'unknown or N/A', 'yes'}
fear_of_falling in {1,2,999}
fear_of_falling_txt in {'no', 'unknown or N/A', 'yes'}
physical_activities_no_yes in {1,2,999}
physical_activities_no_yes_txt in {'no', 'unknown or N/A', 'yes'}
mmse >= 0
NOT mmse > 30 & mmse != 999
eyeglasses_yes_no in {1,2,999}
eyeglasses_yes_no_txt in {'no', 'unknown / not examined', 'yes'}
eyeglasses_worn_during_test in {1,2,999}
eyeglasses_worn_during_test_txt in {'no', 'unknown or N/A', 'yes'}
eyeglasses_test_type_txt in {'bifocal/trifocal', 'contact lenses', 'glasses for distance', 'reading glasses', 'unknown', 'Varilux'}
hearing_aid_no_yes in {1,2,999}
hearing_aid_no_yes_txt in {'yes','no','unknown or N/A'}
hearing_aid_side_txt in {'both sides','left','right','unknown'}
hearing_aid_worn_during_test in {1,2,999}
hearing_aid_worn_during_test_txt in {'yes','no','unknown or N/A'}
hearing_aid_which_worn_during_test_txt in {'both sides', 'left', 'right', 'unknown'}
height_m >= 1
NOT height_m > 2 & height_m != 999
weight_kg >= 35
NOT weight_kg > 150 & weight_kg != 999
bmi_kg_m2 > 0
NOT bmi_kg_m2 > 60 & bmi_kg_m2 != 999
leg_right > 0
NOT leg_right > 120 & leg_right != 999
leg_left > 0
NOT leg_left > 120 & leg_left != 999

-- multivariate: gender
NOT gender in {'f','F'} & gender_txt == 'Male'
NOT gender in {'m','M'} & gender_txt == 'Female'

-- multivariate: test indication
NOT test_indication == 1 & test_indication_txt != 'Memory Clinic routine'
NOT test_indication == 2 & test_indication_txt != 'Study'
NOT test_indication == 3 & test_indication_txt != 'Suspected NPH'
NOT test_indication == 4 & test_indication_txt != 'Inpatient'
NOT test_indication == 5 & test_indication_txt != 'Outpatient'
NOT test_indication == 999 & test_indication_txt != 'unknown'

-- multivariate: living status i
NOT living_status_i == 1 & living_status_i_txt != 'house'
NOT living_status_i == 2 & living_status_i_txt != 'apartment'
NOT living_status_i == 3 & living_status_i_txt != 'senior residence'
NOT living_status_i == 4 & living_status_i_txt != 'skilled nursing facility, nursing home'
NOT living_status_i == 5 & living_status_i_txt != 'other (incl. Convent, assisted living)'
NOT living_status_i == 999 & living_status_i_txt != 'unknown'

-- multivariate: living status ii
NOT living_status_ii == 1 & living_status_ii_txt != 'alone'
NOT living_status_ii == 2 & living_status_ii_txt != 'with partner'
NOT living_status_ii == 3 & living_status_ii_txt != 'with child/ren'
NOT living_status_ii == 4 & living_status_ii_txt != 'with relatives'
NOT living_status_ii == 5 & living_status_ii_txt != 'roommates (WG)'
NOT living_status_ii == 6 & living_status_ii_txt != 'other'
NOT living_status_ii == 999 & living_status_ii_txt != 'unknown'

-- multivariate: living status i and ii
NOT living_status_i in {3,4,5} & living_status_ii in {3,4}
NOT living_status_i == 3 & living_status_ii == 5
NOT living_status_i in {3,5} & living_status_ii == 7

-- multivariate: previous falls
NOT previous_fall_12_months == 1 & previous_fall_12_months_txt != 'yes'
NOT previous_fall_12_months == 2 & previous_fall_12_months_txt != 'no'
NOT previous_fall_12_months == 999 & previous_fall_12_months_txt != 'unknown or N/A'
NOT previous_fall_s == 1 & previous_fall_s_txt != 'yes'
NOT previous_fall_s == 2 & previous_fall_s_txt != 'no'
NOT previous_fall_s == 999 & previous_fall_s_txt != 'unknown or N/A'
NOT previous_fall_12_months == 1 & previous_fall_s != 1
NOT previous_fall_12_months != 2 & previous_fall_s == 2

-- multivariate: fear of falling
NOT fear_of_falling == 1 & fear_of_falling_txt != 'yes'
NOT fear_of_falling == 2 & fear_of_falling_txt != 'no'
NOT fear_of_falling == 999 & fear_of_falling_txt != 'unknown or N/A'

-- multivariate: physical activities
NOT physical_activities_no_yes == 1 & physical_activities_no_yes_txt != 'yes'
NOT physical_activities_no_yes == 2 & physical_activities_no_yes_txt != 'no'
NOT physical_activities_no_yes == 999 & physical_activities_no_yes_txt != 'unknown or N/A'

-- multivariate: eyeglasses
NOT eyeglasses_worn_during_test == 1 & eyeglasses_worn_during_test_txt != 'yes'
NOT eyeglasses_worn_during_test == 2 & eyeglasses_worn_during_test_txt != 'no'
NOT eyeglasses_worn_during_test == 999 & eyeglasses_worn_during_test_txt != 'unknown or N/A'
NOT eyeglasses_worn_during_test != 1 & eyeglasses_test_type_txt != 'unknown'
NOT eyeglasses_yes_no == 1 & eyeglasses_yes_no_txt != 'yes'
NOT eyeglasses_yes_no == 2 & eyeglasses_yes_no_txt != 'no'
NOT eyeglasses_yes_no == 999 & eyeglasses_yes_no_txt != 'unknown / not examined'
NOT eyeglasses_worn_during_test == 1 & eyeglasses_yes_no != 1
NOT eyeglasses_yes_no == 2 & eyeglasses_worn_during_test != 2

-- multivariate: hearing aid
NOT hearing_aid_worn_during_test == 1 & hearing_aid_worn_during_test_txt != 'yes'
NOT hearing_aid_worn_during_test == 2 & hearing_aid_worn_during_test_txt != 'no'
NOT hearing_aid_worn_during_test == 999 & hearing_aid_worn_during_test_txt != 'unknown or N/A'
NOT hearing_aid_worn_during_test != 1 & hearing_aid_which_worn_during_test_txt != 'unknown'
NOT hearing_aid_no_yes == 1 & hearing_aid_no_yes_txt != 'yes'
NOT hearing_aid_no_yes == 2 & hearing_aid_no_yes_txt != 'no'
NOT hearing_aid_no_yes == 999 & hearing_aid_no_yes_txt != 'unknown or N/A'
NOT hearing_aid_worn_during_test == 1 & hearing_aid_no_yes != 1
NOT hearing_aid_worn_during_test != 2 & hearing_aid_no_yes == 2
NOT hearing_aid_which_worn_during_test_txt == 'both sides' & hearing_aid_side_txt != 'both sides'
NOT hearing_aid_which_worn_during_test_txt == 'left' & hearing_aid_side_txt == 'right'
NOT hearing_aid_which_worn_during_test_txt == 'left' & hearing_aid_side_txt == 'unknown'
NOT hearing_aid_which_worn_during_test_txt == 'right' & hearing_aid_side_txt == 'left'
NOT hearing_aid_which_worn_during_test_txt == 'right' & hearing_aid_side_txt == 'unknown'
NOT hearing_aid_no_yes != 1 & hearing_aid_side_txt != 'unknown'

--multivariate: weight & length
NOT height_m == 999 & bmi_kg_m2 != 999
NOT weight_kg == 999 & bmi_kg_m2 != 999

-- multivariate: legs
NOT leg_left != 999 & leg_left > S^30(leg_right)
NOT leg_right != 999 & leg_left < S^-30(leg_right)

-- transition: gender
NOT gender#curr in {'F', 'f'} & gender#next in {'M', 'm'}
NOT gender#curr in {'M', 'm'} & gender#next in {'F', 'f'}

-- transition: falls & eyeglasses & hearing aids
NOT previous_fall_s#curr == 1 & previous_fall_s#next != 1
NOT eyeglasses_yes_no#curr == 1 & eyeglasses_yes_no#next != 1
NOT hearing_aid_no_yes#curr == 1 & hearing_aid_no_yes#next != 1
NOT hearing_aid_side_txt#curr == 'both sides' & hearing_aid_side_txt#next != 'both sides'
NOT hearing_aid_side_txt#curr == 'left' & hearing_aid_side_txt#next == 'right'
NOT hearing_aid_side_txt#curr == 'left' & hearing_aid_side_txt#next == 'unknown'
NOT hearing_aid_side_txt#curr == 'right' & hearing_aid_side_txt#next == 'left'
NOT hearing_aid_side_txt#curr == 'right' & hearing_aid_side_txt#next == 'unknown'

-- transition: mmse
NOT mmse#next != 999 & mmse#next > S^3(mmse#curr)
NOT mmse#curr != 999 & mmse#next < S^-3(mmse#curr)

-- transition: height & weight & bmi
NOT height_m#next != 999 & height_m#next > S^300(height_m#curr)
NOT height_m#curr != 999 & height_m#next < S^-400(height_m#curr)
NOT weight_kg#next != 999 & weight_kg#next > S^400(weight_kg#curr)
NOT weight_kg#curr != 999 & weight_kg#next < S^-500(weight_kg#curr)
NOT weight_kg#next > weight_kg#curr & height_m#next <= height_m#curr & bmi_kg_m2#next < bmi_kg_m2#curr
NOT weight_kg#next <= weight_kg#curr & height_m#next > height_m#curr & bmi_kg_m2#next > bmi_kg_m2#curr

-- transition: legs
NOT leg_right#next != 999 & leg_right#next > S^50(leg_right#curr)
NOT leg_right#curr != 999 & leg_right#next < S^-50(leg_right#curr)



