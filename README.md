# TT_CropMarket

판매량에 따라 실시간으로 물가가 변동하는 동적 농산물 시장 플러그인입니다.
플레이어가 작물을 많이 팔수록 가격이 내려가고, 시간이 지나면 자연 회복됩니다.
등급(일반 / 은 / 금)별로 독립적인 가격 흐름과 시장 붕괴 시스템을 갖추고 있습니다.

---

## 주요 기능

- **동적 가격 시스템** — 판매할수록 가격 하락, 시간이 지나면 자동 회복
- **3단계 등급** — 일반 / 은 / 금 등급별 독립 가격 운영
- **묶음 판매/구매** — 등급별·씨앗별 묶음 수량을 config에서 자유롭게 설정
- **시장 붕괴** — 고가 구간에서 판매 시 확률적으로 가격 폭락, 일정 시간 후 자동 복구
- **세금 시스템** — 기본 세율 / 권한 보유자 감면 세율 지원
- **씨앗 상점** — 수량 선택형 씨앗 구매 GUI 지원
- **페이지네이션** — 페이지당 28개, 초과 시 이전/다음 페이지 버튼 자동 생성
- **다양한 아이템 타입** — 바닐라 / ItemsAdder / MMOItems 아이템 모두 지원
- **파일 로그** — 판매·회복·붕괴 이벤트를 날짜별 로그 파일로 기록
- **비동기 저장** — 데이터 I/O를 비동기로 처리하여 서버 성능 영향 최소화

---

## 의존 플러그인

| 플러그인 | 용도 | 필수 여부 |
|----------|------|-----------|
| [Vault](https://www.spigotmc.org/resources/vault.34315/) | 경제 시스템 연동 | **필수** |
| [ItemsAdder](https://www.spigotmc.org/resources/itemsadder.73355/) | ItemsAdder 커스텀 아이템 사용 시 | 권장 |
| [MMOItems](https://www.spigotmc.org/resources/mmoitems-premium.39267/) | MMOItems 커스텀 아이템 사용 시 | 권장 |

> ItemsAdder / MMOItems가 없어도 플러그인은 정상 작동합니다.
> 해당 플러그인의 아이템을 config에서 사용할 경우에만 설치하면 됩니다.

---

## 설치 방법

1. 위 의존 플러그인을 설치합니다.
2. `TT_CropMarket.jar`를 `plugins/` 폴더에 넣습니다.
3. 서버를 재시작합니다.
4. `plugins/TT_CropMarket/config.yml`에서 작물·가격·세율 등을 설정합니다.

---

## 명령어 및 권한

| 명령어 | 설명 | 권한 |
|--------|------|------|
| `/농작물` | 농산물 시장 GUI 열기 | `cropmarket.use` (기본: 모든 플레이어) |
| `/농작물관리 reload` | 설정 파일 리로드 | `cropmarket.admin` (기본: OP) |
| `/농작물관리 info` | 현재 가격 정보 출력 | `cropmarket.admin` |
| `/농작물관리 reset <작물> <등급>` | 특정 작물 가격 초기화 | `cropmarket.admin` |
| `/농작물관리 set <작물> <등급> <가격>` | 특정 작물 가격 직접 설정 | `cropmarket.admin` |

---

## 설정 파일 주요 항목

`plugins/TT_CropMarket/config.yml`에서 모든 수치를 조정할 수 있습니다.

### 작물 아이템 타입 설정

`item-type`을 명시하거나 생략하면 자동으로 감지됩니다.

```yaml
crops:
  diamond_ore:
    display-name: "§b다이아몬드 광석"
    icon: DIAMOND_ORE
    seed-id: "customcrops:diamond_seeds"
    normal:
      item-type: "VANILLA"      # 바닐라 아이템
      item-id: "DIAMOND"
      base-price: 1000.0
      min-price: 200.0
      max-price: 2000.0
    silver:
      item-type: "ITEMSADDER"   # ItemsAdder 아이템 (또는 생략 — ":" 포함 시 자동 감지)
      item-id: "myplugin:silver_diamond"
      base-price: 5000.0
      min-price: 1000.0
      max-price: 15000.0
    gold:
      item-type: "MMOITEMS"     # MMOItems 아이템 (또는 생략 — mmoitems-type 있을 시 자동 감지)
      item-id: "GOLDEN_DIAMOND"
      mmoitems-type: "GEM"
      base-price: 10000.0
      min-price: 2000.0
      max-price: 35000.0
```

**자동 감지 규칙 (item-type 생략 시)**

| 조건 | 감지 결과 |
|------|-----------|
| `mmoitems-type` 항목이 있는 경우 | MMOITEMS |
| `item-id`에 `:` 포함 (예: `customcrops:tomato`) | ITEMSADDER |
| 그 외 | VANILLA |

### 묶음 판매/구매 수량

```yaml
# 판매 묶음 수량 (등급별)
sell-amount:
  normal: 64
  silver: 8
  gold: 4

# 씨앗 구매 수량 선택지 (최대 3개)
seeds:
  default-price: 100.0
  buy-amounts: [4, 8, 64]
```

### 가격 시스템

```yaml
# 등급별 가격 회복 주기 (분)
recovery:
  normal:  { min-minutes: 1,  max-minutes: 15 }
  silver:  { min-minutes: 10, max-minutes: 60 }
  gold:    { min-minutes: 10, max-minutes: 90 }

# 판매 시 가격 하락폭 (%) — 회복 시에도 동일 범위 적용
price-adjustment:
  normal:  { min-percent: 1.0,  max-percent: 3.0  }
  silver:  { min-percent: 7.0,  max-percent: 15.0 }
  gold:    { min-percent: 10.0, max-percent: 75.0 }

# 세금
tax:
  default-rate: 10.0
  reduced-rate: 5.0
  reduced-permission: "권한.노드"
```

> 각 항목에 대한 상세 설명은 config.yml 내 주석을 참고하세요.

---

## 페이지네이션

작물 또는 씨앗이 28개를 초과하면 자동으로 이전/다음 페이지 버튼이 생성됩니다.
별도 설정 없이 작물을 추가하기만 하면 됩니다.

---

## 로그 파일

판매·회복·붕괴 이벤트는 `plugins/TT_CropMarket/logs/market-YYYY-MM-DD.log`에 날짜별로 기록됩니다.

```
[14:23:01] [판매] 플레이어: Steve | 토마토 (일반등급) x64개 | 판매가: 3,200원 | 세금: 320원 | 실수령: 2,880원 | 하락: 2.3% | 판매 후 가격: 48원
[14:31:45] [회복] 일반등급 | 토마토 | 48원 → 49원 (+2.1%)
[15:02:10] [붕괴] 플레이어: Alex | 옥수수 (금등급) | 붕괴 전 가격: 16,800원 | 발동 확률: 18.5%
```

---

## 개발 환경

- **Minecraft** : 1.20.1
- **Java** : 17

다른 버전에 대한 포팅 및 수정은 지원하지 않습니다.
다른 버전이 필요하다면 소스코드를 직접 수정하여 사용해주세요.

---

## 라이선스

이 플러그인은 개인 서버 용도로 자유롭게 사용할 수 있습니다.
무단 재배포 및 상업적 이용은 금지합니다.
