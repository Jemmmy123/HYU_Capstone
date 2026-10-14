# 정확도 평가

- `make_dataset.py`: 라벨이 붙은 개발용 평가 세트(`dataset.tsv`, 5,189단어)를 만듭니다. 시드가 고정돼 있습니다.
  - 민감 420단어: 카드번호(Luhn 맞는 테스트 번호), 전화번호(한국·미국 형식), 이메일, SSN, 비밀번호, 계좌·ID, PIN, 트리거 단어 뒤 비밀 단어. 모두 가짜 값입니다.
  - 일반: 영어 빈도 상위 3,000단어(wordfreq) + 헷갈리기 쉬운 문장 60개("password manager", "card game", iPhone, COVID-19, Office365, 날짜·시간·가격 등)
- `make_holdout.py`: 확인용 세트(`holdout.tsv`, 4,696단어, 민감 348). **규칙을 고치기 전에** 다른 문장·단어로 만들어 고정했고, 고치는 데는 쓰지 않았습니다. 고친 뒤 한 번만 평가했습니다.
- `Evaluate.kt`: 키보드와 같은 방식(앞 단어 문맥 포함)으로 `SensitiveWordDetector`를 돌려 정밀도·재현율·F1·오탐률과 속도를 계산합니다. 단어 전체를 보는 "탐지기" 기준과, 키보드가 기호에서 단어를 끊는 방식까지 흉내 낸 "키보드" 기준(실제로 무엇이 학습되는지) 두 가지로 셉니다.
- `after_holdout.md`: 확인용 세트에서 최종 규칙의 결과. 키보드 기준 정밀도 0.967, 재현율 0.941, 오탐률 0.14% (개선 전 규칙: 0.919, 0.606, 0.23%).

데이터 세트(.tsv)는 스크립트로 다시 만들 수 있어서 저장소에 넣지 않았습니다. wordfreq 버전에 따라 일반 단어 목록이 조금 달라질 수 있습니다.

다시 돌리는 법:
```
pip install wordfreq
cd eval
python3 make_dataset.py && python3 make_holdout.py
P=../app/src/main/java/helium314/keyboard/latin/privacy
kotlinc $P/SensitiveWordDetector.kt $P/UserBlockList.kt Evaluate.kt -d out
kotlin -cp out EvaluateKt . holdout.tsv new after_holdout.md
```
