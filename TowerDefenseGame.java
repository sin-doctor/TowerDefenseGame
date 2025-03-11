import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.ArrayList;
import java.util.Random;

// 유닛의 기본 클래스
class Unit {
    int x, y, health, damage, speed;
    boolean isPlayerUnit;
    int attackCooldown = 1000; // 공격 주기 (1초)
    long lastAttackTime = 0; // 마지막 공격 시간

    public Unit(int x, int y, int health, int damage, int speed, boolean isPlayerUnit) {
        this.x = x;
        this.y = y;
        this.health = health;
        this.damage = damage;
        this.speed = speed;
        this.isPlayerUnit = isPlayerUnit;
    }

    public void move() {
        if (isPlayerUnit) {
            x += speed; // 플레이어 유닛은 오른쪽으로 이동
        } else {
            x -= speed; // 적 유닛은 왼쪽으로 이동
        }
    }

    public void takeDamage(int damage) {
        health -= damage;
    }

    public boolean isAlive() {
        return health > 0;
    }

    // 공격 가능 여부 확인 (공격 주기마다 공격)
    public boolean canAttack() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAttackTime >= attackCooldown) {
            lastAttackTime = currentTime;
            return true;
        }
        return false;
    }

    // 충돌 감지
    public boolean isColliding(Unit other) {
        return Math.abs(this.x - other.x) < 20 && Math.abs(this.y - other.y) < 20;
    }

    // 기지와의 충돌 감지
    public boolean isColliding(Base base) {
        return Math.abs(this.x - base.x) < 25 && Math.abs(this.y - base.y) < 25;
    }

    public void draw(Graphics g) {
        // 기본적으로 직사각형으로 그립니다. 자식 클래스에서 오버라이드 가능합니다.
        g.fillRect(x - 10, y - 10, 20, 20);
        drawHealthBar(g); // 체력 바 그리기
    }

    // 체력 바 그리기
    public void drawHealthBar(Graphics g) {
        g.setColor(Color.RED);
        g.fillRect(x - 10, y - 20, 20, 5); // 체력 바 배경
        g.setColor(Color.GREEN);
        g.fillRect(x - 10, y - 20, 20 * health / 100, 5); // 체력 바
    }
}

// 기본 유닛 클래스 (근거리 공격)
class BasicUnit extends Unit {
    public BasicUnit(int x, int y, boolean isPlayerUnit) {
        super(x, y, 100, 20, 2, isPlayerUnit);
    }

    @Override
    public void draw(Graphics g) {
        g.setColor(Color.GREEN);
        super.draw(g); // 기본 클래스의 draw 메서드를 호출
    }
}

// 탱커 유닛 클래스 (체력이 높고 공격력은 낮음)
class TankerUnit extends Unit {
    public TankerUnit(int x, int y, boolean isPlayerUnit) {
        super(x, y, 200, 10, 1, isPlayerUnit);
    }

    @Override
    public void draw(Graphics g) {
        g.setColor(Color.BLUE);
        super.draw(g); // 기본 클래스의 draw 메서드를 호출
    }
}

// 원거리 유닛 클래스 (사정거리가 긴 유닛)
class RangedUnit extends Unit {
    int range;

    public RangedUnit(int x, int y, boolean isPlayerUnit) {
        super(x, y, 100, 30, 1, isPlayerUnit);
        this.range = 100; // 사정거리
    }

    @Override
    public void draw(Graphics g) {
        g.setColor(Color.ORANGE);
        super.draw(g); // 기본 클래스의 draw 메서드를 호출
    }

    // 원거리 공격 메서드
    public void attack(Unit target) {
        if (canAttack() && canAttack(target)) {
            target.takeDamage(damage);
        }
    }

    // 원거리 공격 가능한 범위 내에 적이 있는지 확인
    public boolean canAttack(Unit other) {
        return Math.abs(this.x - other.x) <= range; // 사정거리를 100으로 설정
    }
}

// 기지 클래스
class Base {
    int x, y, health;
    boolean isPlayerBase;

    public Base(int x, int y, int health, boolean isPlayerBase) {
        this.x = x;
        this.y = y;
        this.health = health;
        this.isPlayerBase = isPlayerBase;
    }

    public void takeDamage(int damage) {
        health -= damage;
    }

    public boolean isDestroyed() {
        return health <= 0;
    }

    public void draw(Graphics g) {
        g.setColor(isPlayerBase ? Color.BLUE : Color.RED);
        g.fillRect(x - 25, y - 25, 50, 50); // 기지 그리기
    }
}

// 게임 패널 클래스
class GamePanel extends JPanel implements ActionListener {
    ArrayList<Unit> playerUnits = new ArrayList<>();
    ArrayList<Unit> enemyUnits = new ArrayList<>();
    Timer timer;
    Base playerBase;
    Base enemyBase;
    int playerGold = 100; // 플레이어의 자원 (골드)
    int spawnCooldown = 0; // 유닛 생성 쿨다운
    int timeElapsed = 0; // 경과 시간
    int goldUpgradeLevel = 0; // 자원 업그레이드 레벨
    double goldMultiplier = 1.0; // 자원 획득 배율
    Random random = new Random(); // 랜덤 생성기
    double speedMultiplier = 1.0; // 게임 속도 배속

    public GamePanel() {
        this.setPreferredSize(new Dimension(800, 600));
        playerBase = new Base(50, 300, 1000, true); // 플레이어 기지
        enemyBase = new Base(750, 300, 1000, false); // 적 기지

        timer = new Timer(50, this); // 게임 루프 (0.05초마다 실행)
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // 배경
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(0, 0, getWidth(), getHeight());

        // 기지 그리기
        playerBase.draw(g);
        enemyBase.draw(g);

        // 플레이어 유닛 그리기
        for (Unit unit : playerUnits) {
            unit.draw(g);
        }

        // 적 유닛 그리기
        for (Unit unit : enemyUnits) {
            unit.draw(g);
        }

        // 자원 및 정보 표시
        g.setColor(Color.BLACK);
        g.drawString("Gold: " + playerGold, 10, 20);
        g.drawString("Gold Multiplier: " + goldMultiplier, 10, 40);
        g.drawString("Player Base HP: " + playerBase.health, 10, 60);
        g.drawString("Enemy Base HP: " + enemyBase.health, 10, 80);
        g.drawString("Upgrade Cost (1st): 200 Gold", 10, 100);
        g.drawString("Upgrade Cost (2nd): 250 Gold", 10, 120);
        g.drawString("Upgrade Level: " + goldUpgradeLevel + "/5", 10, 140); // 업그레이드 레벨 표시
        g.drawString("Game Speed: " + speedMultiplier + "x", 10, 160); // 게임 속도 표시
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        timeElapsed++;

        // 쿨다운 업데이트
        updateCooldown();

        // 유닛 이동 및 전투 처리
        handleCombatAndMovement();

        // 자원 증가
        if (timeElapsed % (int)(20 / speedMultiplier) == 0) {
            playerGold += (int)(10 * goldMultiplier); // 자원 획득량에 배율 적용
        }

        // 적 유닛 생성 (일정 시간마다, 난이도에 따라 다르게)
        if (timeElapsed % (int)(60 / speedMultiplier) == 0) {
            spawnEnemyUnit();
        }

        // 기지 공격 처리
        handleBaseAttack();

        // 게임 종료 처리
        if (playerBase.isDestroyed()) {
            System.out.println("게임 오버! 적이 승리했습니다!");
            System.exit(0);
        }
        if (enemyBase.isDestroyed()) {
            System.out.println("승리했습니다!");
            System.exit(0);
        }

        // 화면 갱신
        repaint();
    }

    // 유닛 이동 및 전투 처리
    private void handleCombatAndMovement() {
        ArrayList<Unit> deadPlayerUnits = new ArrayList<>();
        ArrayList<Unit> deadEnemyUnits = new ArrayList<>();

        // 플레이어 유닛 전투 및 이동 처리
        for (Unit playerUnit : playerUnits) {
            boolean foundEnemy = false;
            for (Unit enemyUnit : enemyUnits) {
                if (playerUnit.isColliding(enemyUnit)) {
                    // 적 유닛과 충돌한 경우 전투
                    foundEnemy = true; // 적 유닛과 충돌했음을 표시

                    // 공격
                    if (playerUnit.canAttack()) {
                        enemyUnit.takeDamage(playerUnit.damage);
                    }
                    if (!enemyUnit.isAlive()) {
                        deadEnemyUnits.add(enemyUnit);
                    }
                }

                // 원거리 유닛의 경우
                if (playerUnit instanceof RangedUnit) {
                    RangedUnit rangedUnit = (RangedUnit) playerUnit;
                    // 적 유닛과의 거리 체크
                    if (rangedUnit.canAttack(enemyUnit) && !foundEnemy) {
                        rangedUnit.attack(enemyUnit);
                        if (!enemyUnit.isAlive()) {
                            deadEnemyUnits.add(enemyUnit);
                        }
                    }
                }
            }

            // 생존한 유닛이 적 유닛과 충돌하지 않는 경우 타워 공격
            if (playerUnit.isAlive()) {
                if (!foundEnemy) {
                    // 적 유닛이 없을 경우 기지 공격
                    if (playerUnit.isColliding(enemyBase)) {
                        enemyBase.takeDamage(playerUnit.damage); // 기지 공격
                    } else {
                        playerUnit.move(); // 한 칸 전진
                    }
                }
            }
        }

        // 생존한 적 유닛 이동 및 전투 처리
        for (Unit enemyUnit : enemyUnits) {
            boolean foundPlayer = false;
            for (Unit playerUnit : playerUnits) {
                if (enemyUnit.isColliding(playerUnit)) {
                    foundPlayer = true; // 플레이어 유닛을 찾았음을 표시

                    // 공격
                    if (enemyUnit.canAttack()) {
                        playerUnit.takeDamage(enemyUnit.damage);
                    }
                    if (!playerUnit.isAlive()) {
                        deadPlayerUnits.add(playerUnit);
                    }
                }
            }

            // 유닛이 생존하고 플레이어 유닛과 충돌하지 않는 경우 타워 공격
            if (enemyUnit.isAlive()) {
                if (!foundPlayer) {
                    // 플레이어 유닛이 없을 경우 기지 공격
                    if (enemyUnit.isColliding(playerBase)) {
                        playerBase.takeDamage(enemyUnit.damage); // 기지 공격
                    } else {
                        enemyUnit.move(); // 한 칸 전진
                    }
                }
            }
        }

        // 사망한 유닛 목록에서 제거
        playerUnits.removeAll(deadPlayerUnits);
        enemyUnits.removeAll(deadEnemyUnits);
    }

    // 기지 공격 처리 메서드
    private void handleBaseAttack() {
        for (Unit playerUnit : playerUnits) {
            if (playerUnit.isColliding(enemyBase)) {
                // 기지에 닿으면 공격
                enemyBase.takeDamage(playerUnit.damage);
            }
        }

        for (Unit enemyUnit : enemyUnits) {
            if (enemyUnit.isColliding(playerBase)) {
                // 기지에 닿으면 공격
                playerBase.takeDamage(enemyUnit.damage);
            }
        }
    }

    // 플레이어 유닛 생성
    public void spawnPlayerUnit(String unitType) {
        if (spawnCooldown > 0) return;

        if (unitType.equals("Basic") && playerGold >= 50) {
            playerUnits.add(new BasicUnit(playerBase.x + 30, playerBase.y, true));
            playerGold -= 50;
        } else if (unitType.equals("Tanker") && playerGold >= 100) {
            playerUnits.add(new TankerUnit(playerBase.x + 30, playerBase.y, true));
            playerGold -= 100;
        } else if (unitType.equals("Ranged") && playerGold >= 150) {
            playerUnits.add(new RangedUnit(playerBase.x + 30, playerBase.y, true));
            playerGold -= 150;
        }
    }

    // 자원 획득 속도 업그레이드
    public void upgradeGoldMultiplier() {
        if (goldUpgradeLevel < 5) {
            if (goldUpgradeLevel == 0 && playerGold >= 200) {
                goldMultiplier = 1.2;
                playerGold -= 200;
                goldUpgradeLevel++;
            } else if (goldUpgradeLevel == 1 && playerGold >= 250) {
                goldMultiplier = 1.4;
                playerGold -= 250;
                goldUpgradeLevel++;
            } else if (goldUpgradeLevel == 2 && playerGold >= 300) {
                goldMultiplier = 1.6;
                playerGold -= 300;
                goldUpgradeLevel++;
            } else if (goldUpgradeLevel == 3 && playerGold >= 350) {
                goldMultiplier = 1.8;
                playerGold -= 350;
                goldUpgradeLevel++;
            } else if (goldUpgradeLevel == 4 && playerGold >= 400) {
                goldMultiplier = 2.0;
                playerGold -= 400;
                goldUpgradeLevel++;
            }
        }
    }

    // 적 유닛 생성 (일정 시간마다, 난이도에 따라 다르게)
    private void spawnEnemyUnit() {
        int enemyType;
        // 시간에 따라 유닛의 종류를 다양하게 설정 (60초마다 난이도 상승)
        if (timeElapsed < 120) { // 2분이 되기 전까지
            enemyType = 0; // 기본 유닛
        } else if (timeElapsed < 240) { // 4분이 되기 전까지
            enemyType = random.nextInt(2); // 기본 또는 탱커 유닛
        } else {
            enemyType = random.nextInt(3); // 기본, 탱커 또는 원거리 유닛
        }

        if (enemyType == 0) {
            enemyUnits.add(new BasicUnit(enemyBase.x - 30, enemyBase.y, false));
        } else if (enemyType == 1) {
            enemyUnits.add(new TankerUnit(enemyBase.x - 30, enemyBase.y, false));
        } else if (enemyType == 2) {
            enemyUnits.add(new RangedUnit(enemyBase.x - 30, enemyBase.y, false));
        }
    }

    // 쿨다운 업데이트
    public void updateCooldown() {
        if (spawnCooldown > 0) {
            spawnCooldown--;
        }
    }

    // 게임 속도 조절
    public void setGameSpeed(double speed) {
        speedMultiplier = speed;
        timer.setDelay((int)(50 / speed)); // 타이머 딜레이 조정
    }
}

// 메인 클래스
public class TowerDefenseGame extends JFrame {
    GamePanel gamePanel;

    public TowerDefenseGame() {
        gamePanel = new GamePanel();
        this.add(gamePanel);
        this.setTitle("타워 디펜스 게임");
        this.pack();
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null); // 창을 화면 가운데에 위치
        this.setVisible(true);

        // 유닛 생성 버튼
        JPanel buttonPanel = new JPanel();
        JButton basicButton = new JButton("기본 유닛 생성 (50 골드)");
        JButton tankerButton = new JButton("탱커 유닛 생성 (100 골드)");
        JButton rangedButton = new JButton("원거리 유닛 생성 (150 골드)");
        JButton upgradeGoldButton = new JButton("자원 획득 업그레이드");
        JButton speed1_2xButton = new JButton("1.2배속");
        JButton speed1_5xButton = new JButton("1.5배속");
        JButton resetSpeedButton = new JButton("정상속도");

        basicButton.addActionListener(e -> gamePanel.spawnPlayerUnit("Basic"));
        tankerButton.addActionListener(e -> gamePanel.spawnPlayerUnit("Tanker"));
        rangedButton.addActionListener(e -> gamePanel.spawnPlayerUnit("Ranged"));
        upgradeGoldButton.addActionListener(e -> gamePanel.upgradeGoldMultiplier());
        speed1_2xButton.addActionListener(e -> gamePanel.setGameSpeed(1.2));
        speed1_5xButton.addActionListener(e -> gamePanel.setGameSpeed(1.5));
        resetSpeedButton.addActionListener(e -> gamePanel.setGameSpeed(1.0)); // 기본 속도

        buttonPanel.add(basicButton);
        buttonPanel.add(tankerButton);
        buttonPanel.add(rangedButton);
        buttonPanel.add(upgradeGoldButton);
        buttonPanel.add(speed1_2xButton);
        buttonPanel.add(speed1_5xButton);
        buttonPanel.add(resetSpeedButton); // 기본 속도 버튼 추가

        this.add(buttonPanel, BorderLayout.SOUTH);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TowerDefenseGame());
    }
}
