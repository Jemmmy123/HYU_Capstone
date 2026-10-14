# holdout.tsv, mode new

| view | units | TP | FN | FP | TN | precision | recall | F1 | false positive rate |
|---|---|---|---|---|---|---|---|---|---|
| detector (whole tokens) | 4696 | 337 | 11 | 6 | 4342 | 0.983 | 0.968 | 0.975 | 0.14% |
| keyboard (what gets learned) | 4502 | 177 | 11 | 6 | 4308 | 0.967 | 0.941 | 0.954 | 0.14% |

Keyboard view counts only tokens with at least one word the keyboard can learn (starts with a letter). TP = sensitive token with nothing learned, FN = some part learned, FP = normal token with a part blocked.

Keyboard view, sensitive tokens kept out of learning per category:
- account: 22/22
- email: 36/36
- password: 47/58
- secret: 72/72

Keyboard view, wrongly blocked per normal source:
- normal_sentence: 6/303
- template: 0/1020
- wordfreq_3001_6000: 0/2991

## Leaked (keyboard view), unique
- [password] `diamondcookie` learns [diamondcookie] in "diamondcookie is the new one"
- [password] `sparklesoccer` learns [sparklesoccer] in "I set it to sparklesoccer"
- [password] `kittenliverpool` learns [kittenliverpool] in "master key kittenliverpool ok"
- [password] `sparklecookie` learns [sparklecookie] in "master key sparklecookie ok"
- [password] `pumpkindiamond` learns [pumpkindiamond] in "it was pumpkindiamond yesterday"
- [password] `b@n@n@` learns [b@n@n@] in "I set it to b@n@n@"
- [password] `kittenmickey` learns [kittenmickey] in "wifi key kittenmickey"
- [password] `soccerbutterfly` learns [soccerbutterfly] in "it was soccerbutterfly yesterday"
- [password] `bustermustang` learns [bustermustang] in "it was bustermustang yesterday"
- [password] `mickeylemon` learns [mickeylemon] in "I set it to mickeylemon"
- [password] `CpnxtyWKhLDjs` learns [CpnxtyWKhLDjs] in "CpnxtyWKhLDjs is the new one"

## Wrongly blocked (keyboard view), unique
- `fell` (AFTER_TRIGGER_WORD) in "the pin fell off my jacket"
- `as` (AFTER_TRIGGER_WORD) in "use the same login as before"
- `never` (AFTER_TRIGGER_WORD) in "the otp never arrived"
- `Covid19` (PASSWORD_LIKE) in "Covid19 cases went up"
- `Windows11` (PASSWORD_LIKE) in "Windows11 update broke my printer"
- `iPad10` (PASSWORD_LIKE) in "the iPad10 is cheaper now"

## Speed (JVM on the build machine, not the phone)
- detect(): 1.87 us per word; with a 50-word user list: 20.05 us
