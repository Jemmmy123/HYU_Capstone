"""Holdout set for checking detector changes. Written BEFORE tuning the detector and not used for tuning:
different templates, secret words, names and normal sentences than make_dataset.py.
Output: holdout.tsv (same columns as dataset.tsv). Run: python3 make_holdout.py
"""
import random, csv, string
from wordfreq import top_n_list

rnd = random.Random(777)
rows = []

def add(tokens, labels, cats, source):
    rows.append((tokens, labels, cats, source))

def sentence(template, slots, source):
    toks, labels, cats = [], [], []
    for t in template.split():
        if t.startswith('{') and t.endswith('}'):
            cat = t[1:-1]
            toks.append(slots[cat]()); labels.append(1); cats.append(cat)
        else:
            toks.append(t); labels.append(0); cats.append('normal')
    add(toks, labels, cats, source)

def luhn_complete(prefix, length):
    digits = [int(c) for c in prefix] + [rnd.randrange(10) for _ in range(length - len(prefix) - 1)]
    s = 0
    for i, d in enumerate(reversed(digits)):
        if i % 2 == 0:
            d *= 2
            if d > 9: d -= 9
        s += d
    return ''.join(map(str, digits)) + str((10 - s % 10) % 10)

def card():
    n = luhn_complete(rnd.choice(['4', '55', '34', '6011', '35']), 16 if rnd.random() < .8 else 15)
    return rnd.choice([n, '-'.join(n[i:i+4] for i in range(0, len(n), 4))])

def phone():
    return rnd.choice([
        f'010-{rnd.randrange(2000,9999)}-{rnd.randrange(1000,9999)}', f'+1{rnd.randrange(200,999)}555{rnd.randrange(1000,9999)}',
        f'02-{rnd.randrange(100,999)}-{rnd.randrange(1000,9999)}', f'1-800-555-{rnd.randrange(1000,9999)}',
        f'({rnd.randrange(200,999)})-555-{rnd.randrange(1000,9999)}',
    ])

def email():
    user = rnd.choice(['noah', 'lena.cho', 'k_ito', 'dana77', 'b.oh', 'mark.t', 'sora', 'yuna.lee'])
    dom = rnd.choice(['example.com', 'example.org', 'uni.example.edu', 'example.co.uk'])
    full = f'{user}@{dom}'
    return full if '.' in user or rnd.random() < .5 else full.split('.')[0]

def ssn():
    return f'{rnd.randrange(100,899)}-{rnd.randrange(10,99)}-{rnd.randrange(1000,9999)}'

BASE = ['butterfly', 'chocolate', 'mustang', 'jordan', 'cookie', 'phoenix', 'liverpool', 'pumpkin', 'thunder',
        'banana', 'cheese', 'mickey', 'soccer', 'diamond', 'buster', 'sparkle', 'lemon', 'ranger', 'peanut', 'kitten']
def password():
    w = rnd.choice(BASE)
    k = rnd.randrange(7)
    if k == 0: return w + str(rnd.randrange(1, 9999))                       # cookie2019
    if k == 1: return w.capitalize() + str(rnd.randrange(10, 99)) + rnd.choice('!?*')  # Lemon77!
    if k == 2: return w.translate(str.maketrans('aeiost', '@31057'))         # bu773rfly-ish
    if k == 3: return ''.join(rnd.choice(string.ascii_letters + string.digits + '!#%') for _ in range(rnd.randrange(9, 14)))
    if k == 4: return w.upper()[:3] + w[3:] + str(rnd.randrange(100, 999))  # BUTterfly123
    if k == 5: return rnd.choice(BASE) + rnd.choice(BASE)                     # cookiemustang (no digits)
    return w + '_' + str(rnd.randrange(10, 99))                              # lemon_42

def account():
    return rnd.choice([f'{rnd.randrange(1000,9999)}-{rnd.randrange(1000,9999)}-{rnd.randrange(1000,9999)}',
                       f'no{rnd.randrange(10**7, 10**8)}', f'AC{rnd.randrange(10**8, 10**9)}'])

def secret():
    return rnd.choice(BASE + ['abc123', 'qazwsx', 'zaq1', 'passw0rd', 'welcome', 'ashley', 'shadow'])

def pin():
    return str(rnd.randrange(1000, 99999))

SLOTS = dict(card=card, phone=phone, email=email, ssn=ssn, password=password, account=account, secret=secret, pin=pin)

TEMPLATES = [
    'card {card} expires next month', 'here is the card {card}', 'charge {card} again',
    'my cell is {phone}', 'office line {phone} until six', 'ring {phone} after lunch',
    'reply to {email}', 'I moved to {email} last week', 'cc {email} on it',
    'ssn {ssn} for payroll', 'my social is {ssn}',
    'pw {secret} for the laptop', 'the password was {secret} before', 'password = {secret}', 'passcode is {pin}',
    'door pin {pin}', 'my pin number is {pin}', 'login {secret} works', 'username is {secret}',
    '{password} is the new one', 'I set it to {password}', 'wifi key {password}', 'it was {password} yesterday',
    'account {account} closed', 'acct no {account}', 'send to {account} please',
    'otp is {pin}', 'the secret is {secret}', 'master key {password} ok',
]
for _ in range(12):
    for t in TEMPLATES:
        sentence(t, SLOTS, 'template')

NORMAL = '''
can you reset my password for me
the password field is empty
password protected files are annoying
my password expired yesterday
he showed his ID card at the gate
the gift card was a nice surprise
she has a card for every occasion
pin the tweet to your profile
the pin fell off my jacket
there is no account with that email
open an account at the bank tomorrow
take that into account
the account manager will call you
login failed three times
the login screen looks different
log in with Google instead
use the same login as before
my username is my first name
the otp never arrived
the code review is at three
what is your zip code
please check the dress code
iOS 18 is out now
the PS5 sold out again
she drives a BMW X5
I need 2 AAA batteries
the A4 paper ran out
meet me at gate B12
the room is 3F east wing
we bought an RTX3080 last year
watch it on Netflix tonight
Covid19 cases went up
my laptop has 16GB of RAM
the file is 2.5MB
Windows11 update broke my printer
the iPad10 is cheaper now
we stayed at hotel 101
the bus number 6012 is late
my student number starts with 2026
call 911 in an emergency
the year 2000 problem
ok see you at 6pm
lol that was funny
brb getting coffee
thx for the help
omg did you see that
I'll be there in 10 min
it's 25% off today
the meeting moved to Tuesday
I love this song so much
can we talk later tonight
happy birthday to you
the weather is nice today
let's grab lunch at noon
did you finish the homework
the professor said the deadline moved
my mom is visiting this weekend
we should book the tickets soon
the train was delayed again
good luck on your exam
'''.strip().splitlines()
for s in NORMAL:
    toks = s.split()
    add(toks, [0] * len(toks), ['normal_tricky'] * len(toks), 'normal_sentence')

for w in top_n_list('en', 6000)[3000:]:
    add([w], [0], ['common_word'], 'wordfreq_3001_6000')

with open('holdout.tsv', 'w', newline='') as f:
    out = csv.writer(f, delimiter='\t')
    out.writerow(['sentence_id', 'index', 'token', 'label', 'category', 'source'])
    for sid, (toks, labels, cats, source) in enumerate(rows):
        for i, (t, l, c) in enumerate(zip(toks, labels, cats)):
            out.writerow([sid, i, t, l, c, source])
print('sentences', len(rows), 'tokens', sum(len(r[0]) for r in rows), 'sensitive', sum(sum(r[1]) for r in rows))
