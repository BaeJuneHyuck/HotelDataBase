# 구현 내용
비즈니스 트랜잭션을 지원하는 호텔관리 시스템을 구현.
 
 ![Preview](https://github.com/BaeJuneHyuck/HotelDataBase/blob/master/capture/ER.png?raw=true)
<그림1. 호텔관리 프로그램이 사용하는 데이터베이스>

위의 그림은 HW5에서 제출한 데이터 베이스의 ER 다이어그램이다. 이번 과제에서는 위의 5개의 테이블을 포함하여 관리자 테이블을 추가하여 관리자가 로그인에 사용하는 정보를 테이블에 저장하도록 하였다.

아래 그림들은 설계를 토대로 만들어진 테이블들의 모습을 보여준다.

![Preview](https://github.com/BaeJuneHyuck/HotelDataBase/blob/master/capture/table_employee.PNG?raw=true)

![Preview](https://github.com/BaeJuneHyuck/HotelDataBase/blob/master/capture/table_guest.PNG?raw=true)

![Preview](https://github.com/BaeJuneHyuck/HotelDataBase/blob/master/capture/table_manager.PNG?raw=true)

![Preview](https://github.com/BaeJuneHyuck/HotelDataBase/blob/master/capture/table_reservation.PNG?raw=true)

![Preview](https://github.com/BaeJuneHyuck/HotelDataBase/blob/master/capture/table_room.PNG?raw=true)

![Preview](https://github.com/BaeJuneHyuck/HotelDataBase/blob/master/capture/table_keeping.PNG?raw=true)
<그림2. SQL developer에서 보인 테이블 구조>

## 관리자의 프로그램 동작 시나리오는 아래와 같다. 
*표시된 기능은 LOCK table 쿼리를 사용하는 트랜잭션이다. 동작 중 다른 트랜잭션은 DB를 사용할 수 없게 하여 동시성을 구현하였다. 또한 모든 트랜잭션이 성공적으로 종료되면 commit() 을 통해 트랜잭션을 확정하도록 구현하였다.

1)	메인 프로그램을 실행한다. 시작화면에서는 관리자 또는 게스트로 로그인 할 수 있다.
2)	관리자 id와 password를 통해 관리자모드로 로그인한다.
2-1) 로그인 성공시 관리자의 이름을 출력한다
2-2) 없는 id를 입력하거나 잘못된 비밀번호를 입력 시 오류 메시지를 출력하고 다시 입력 받는다.
3)	관리자모드가 지원하는 기능은 아래와 같다.
3-1) 전체 방 목록 조회 :  방의 번호, 방 예약 상태, 가격 ,설명이 출력된다.
3-2) 전체 하우스키핑 조회 : 방 번호, 직원 ID, 시작 시간, 종료시간이 출력된다.
3-3) 하우스키핑 할당* : 방 번호, 직원 ID, 시작시간, 종료시간을 입력하여 하우스키핑을 할당한다.
4)	로그아웃을 통해 시작화면으로 돌아간다.

## 또한 호텔 게스트(일반 사용자)의 동작 시나리오는 아래와 같다.

1)	메인 프로그램을 실행한다. 
2)	게스트 id와 password를 통해 게스트모드로 로그인한다.
2-1) 로그인 성공시 게스트의 이름을 출력한다
2-2) 없는 id를 입력하거나 잘못된 비밀번호를 입력 시 오류 메시지를 출력하고 다시 입력 받는다.
3)	게스트모드가 지원하는 기능은 아래와 같다.
3-1) 사용가능 방 목록 조회 : 현재 예약 가능한 방의 번호, 가격 ,설명이 출력된다.

3-2) 방 예약* : 방 번호, 예약 시작날짜, 종료날짜를 입력 받아 예약을 시도한다. 방번호가 존재하지 않거나 날짜의 형식이 (yyyy-mm-dd)꼴이 아니거나, 기간이 잘못된 경우 오류 메시지를 출력한다. 또한 해당 기간에 다른 예약이나 하우스키핑이 이미 존재하는 경우 중복되는 예약/하우스키핑의 날짜를 출력한다.

3-3) 체크인* : 자신의 전체 예약 목록 중 RSTATUS = 0인 예약만 진행된 상태인 예약 정보를 출력하고 입력을 통해 체크인 할 예약을 선택한다. 선택된 예약정보의 상태를 “reserved(0)”에서 “checked in(1)” 으로 업데이트한다.

3-4) 체크인( 예약 없이) * : 현재 시간을 start_time으로 설정하여 새로운 예약정보를 테이블에 추가한다. 해당 기능을 통해 추가된 예약의 RSTATUS는 즉시 1로 생성된다. 일반 방 예약시와 동일하게 입력된 데이터를 확인하고 중복되는 예약/하우스키핑을 확인후 예약 데이터를 테이블에 추가한다.

3-5) 체크아웃( 예약 없이) * : 자신의 전체 예약 목록 중 RSTATUS = 1인 체크인 된 예약 정보를 출력하고 입력을 통해 체크아웃 할 예약을 선택한다. 선택한 예약의 상태를 “checked out(2)” 으로 업데이트하며 예약 정보에서 사용 기간을 구하고 방 정보에서 요금을 얻어와 총 거래 요금을 계산한다. 해당 계산 금액만큼 게스트의 테이블에서 MONEY를 감소시킨다. 돈이 부족한 경우는 고려하지 않고 음수로 데이터베이스에 저장된다. 

3-6) 내 예약 확인 : 전체 예약 정보 중 자신의 예약을 선택하여 방 번호, 예약한 날짜, 예약 시작 시간, 종료시간, 예약 진행 상태를 출력한다.

4)	로그아웃을 통해 시작화면으로 돌아간다.

## 위와 같은 시나리오를 진행하기 위하여 아래와 같은 코드를 작성했다.

1)	HotelManager.java 
	Main 함수가 포함 되어있는 실제 동작 프로그램이다.
프로그램이 시작되면 로고 사운드가 출력되고 DataBase클래스의 객체를 생성, connect() 메소드를 이용하여 DB에 접속한다. 이후 해당 객체는 ManagerClient, GuestClient 클래스를 생성할 때 인자로 전달되고 매니저, 게스트 기능의 사용시 DB 접속을 위해 사용된다. while문을 실행하여 매니저 또는 게스트로 접속하거나 프로그램을 종료한다.

2)	DataBase.java
어플리케이션이 사용하는 테이블의 정보를 기록하기위해 각 테이블에 해당하는 static class들을 선언하였다. 또한 DataBase 타입의 클래스로써 JDBC를 구동하기 위하여 DB의 url, 접속 id, password를 가지고 있다. Connect(), disconnect()의 메소드를 지원한다. 

		conn.setTransactionIsolation(8);
		conn.setAutoCommit(false);

연결 시 위의 메소드를 통해 isolation level을 설정, 각 트랜잭션의 원자성을 보장하도록 하였다.

3)	DateSystem.java
프로그램에서 사용되는 날짜/시간 관리용 클래스 “DateSystem”이 정의되어 있다. 날짜를 입력 받고 올바른 형태가 아닌 경우 재입력을 요구하는 getDate(),  입력 받는 문자열이 “yyyy-mm-dd” 형태로 입력되었는지 확인하는 checkDate(), 두 문자열의 기간을 비교하여 시작 시간이 종료시간보다 뒤에 있는 비정상적인 입력을 확인하는 checkPeriod(), 현재 시간을 리턴하는 getCurrentDatetime()을 사용 할 수 있다.

4)	ManagerClient.java
매니저 기능을 제공하기 위한 클래스 “ManagerClient”가 정의되어 있다. 지원하는 기능들은 매니저 시나리오와 동일하다. 생성시 DataBase객체를 전달받아 데이터베이스를 사용하는 메소드에 사용한다.
5)	GuestClient.java 
게스트 기능을 제공하기 위한 클래스 “GuestClient”가 정의되어 있다. 지원하는 기능들은 게스트 시나리오와 동일하다. 


# 프로그램 데모 소개
 프로그램을 실행하기 위해서는 아래와 같은 과정을 수행한다.

1)	github에서 repository를 clone하거나 직접 소스코드를 다운로드 받는다.
2)	Eclipse for java를 실행하여 프로젝트를 생성, hw6 패키지를 만들고 내부에 소스코드를 추가한다.
3)	이클립스의 메뉴에서 Project – properties - java build path를 선택, build path에 OJDBC7을 포함시킨다.
 
 ![Preview](https://github.com/BaeJuneHyuck/HotelDataBase/blob/master/capture/java_build_path.png?raw=true)

<그림3. java build path>
해당 ojdbc 파일은 https://www.oracle.com/database/technologies/appdev/jdbc-downloads.html 에서 다운로드 할 수 있다.

4)	Eclipse의 Run을 통해 프로그램을 실행한다.

! ) 프로그램 실행 시 로고 사운드가 출력됩니다. 
! ) 소리를 출력하기 위한 javax 모듈이 임포트 되어 있습니다. 과제를 진행한 JRE10.0 에서는 문제가 없었으나 컴파일 에러가 발생시 HotelManager.java에서 playSound() 함수를 삭제해주세요.


# 프로그램 실행

  ![Preview](https://github.com/BaeJuneHyuck/HotelDataBase/blob/master/capture/run1.PNG?raw=true)

<그림4. 프로그램 시작 화면>

이제 커맨드를 입력하여 프로그램을 사용한다.

a)	게스트로 접속하기

 ![Preview](https://github.com/BaeJuneHyuck/HotelDataBase/blob/master/capture/run2.PNG?raw=true)
 
<그림5. 게스트 로그인>

Guest 테이블에 저장 되어있는 id와 패스워드를 사용하여 로그인하였다. 올바르지 않은 정보를 입력 시 오류 메시지를 출력한다.

b)	사용 가능 방 출력
 
  ![Preview](https://github.com/BaeJuneHyuck/HotelDataBase/blob/master/capture/run3.PNG?raw=true)
  
<그림5. 방 목록 출력>

전체 방 목록 중 STATUS가 0, 즉 다른 예약이 없는 방을 출력한다.

c)	방 예약

  ![Preview](https://github.com/BaeJuneHyuck/HotelDataBase/blob/master/capture/run4.PNG?raw=true)
  
<그림6. 예약 성공>

방 번호와 시작시간, 종료시간을 통해 방을 예약한다. 다른 사용자가 방예약을 진행중인 경우 lock 기능을 통해 해당 사용자의 트랜잭션이 종료종료되기까지 대기한다.

 ![Preview](https://github.com/BaeJuneHyuck/HotelDataBase/blob/master/capture/run6.PNG?raw=true) 

<그림7. 예약 실패>

입력한 정보가 다른 예약정보와 겹치는 경우 해당 정보를 출력하고 예약을 실패로 처리한다.
 
  ![Preview](https://github.com/BaeJuneHyuck/HotelDataBase/blob/master/capture/run7.PNG?raw=true)
 
<그림8. 예약 실패, 잘못된 방 번호>

존재하지 않는 방 번호를 입력하거나 입력한 날짜의 형식이 잘못된 경우 오류 메시지를 출력한다.

d)	체크인

 ![Preview](https://github.com/BaeJuneHyuck/HotelDataBase/blob/master/capture/run8.PNG?raw=true)
 
 <그림9. 예약 체크인>

자신의 예약 목록을 출력하고 번호를 입력 받는다. 해당 예약의 상태를 reserved 에서 checked in 으로 변경한다.

e)	체크인( 예약없이 즉시 체크인 )
 
  ![Preview](https://github.com/BaeJuneHyuck/HotelDataBase/blob/master/capture/run9.png?raw=true)
 
<그림10. 예약없이 즉시 체크인>

방 번호와 종료시간을 입력 후 다른 예약과 겹치지 않으면 즉시 체크인한다.




f)	체크아웃

 ![Preview](https://github.com/BaeJuneHyuck/HotelDataBase/blob/master/capture/run10.PNG?raw=true)
 
  <그림11. 체크 아웃>

자신의 예약 중 status가 “checked in” 인 목록을 보여주고 번호를 통해 선택한다. 사용한 기간과 방의 가격을 통해서 전체 비용을 계산한 뒤 사용자 DB에서 money를 감소시킨다.

g)	자신의 예약목록 출력 

 ![Preview](https://github.com/BaeJuneHyuck/HotelDataBase/blob/master/capture/run11.PNG?raw=true)
 
 <그림12. 전체 예약 목록 출력>

자신의 전체 예약 목록을 보여준다. 해당 기능은 사용자 요청 시, 체크아웃 사용시 방 선택을 위해 호출된다.

시나리오에서 명시된 것과 같이 일부 함수는 lock 기능을 사용하여 다른 사용자의 접속을 제한한다. 이를 확인하기 위하여 table lock을 설정, 트랜잭션이 종료되고 의도적으로 추가한 delay() 이후에 commit()이 호출된다. 유저의 트랜잭션이 delay에 의해 commit()되지 않은 동안 다른 유저는 트랜잭션을 시작할 수 없다.

이제 로그아웃을 통해 메인 화면으로 돌아간 뒤 관리자로 로그인 한다. 


a)	로그인
 
  ![Preview](https://github.com/BaeJuneHyuck/HotelDataBase/blob/master/capture/run12.PNG?raw=true)
 
<그림13. 로그아웃, 관리자로 로그인>

게스트 모드에서 로그아웃시 메인 화면으로 돌아가 새로 로그인 할 수 있다.

b)	방 목록 출력
 
  ![Preview](https://github.com/BaeJuneHyuck/HotelDataBase/blob/master/capture/run13.PNG?raw=true)
 
<그림14. 관리자의 방 목록 출력>

고객과 달리 방의 상태를 추가로 확인할 수 있다.

c)	하우스키핑 출력

 ![Preview](https://github.com/BaeJuneHyuck/HotelDataBase/blob/master/capture/run14.PNG?raw=true)
 
<그림15. 하우스키핑 목록 출력>

각 방에 할당된 하우스키핑의 목록을 출력한다. 해당 하우스키핑을 진행하는 직원의 번호가 포함되어 있다.

d)	하우스키핑 할당

 ![Preview](https://github.com/BaeJuneHyuck/HotelDataBase/blob/master/capture/run15.PNG?raw=true)
	 
<그림16. 하우스키핑 할당 성공>

 ![Preview](https://github.com/BaeJuneHyuck/HotelDataBase/blob/master/capture/run16.PNG?raw=true)
 
<그림17. 하우스키핑 할당 실패>

방번호와 하우스키핑을 할당할 직원, 시작시간과 종료시간을 입력하여 하우스키핑을 할당한다. 해당 시간대에 다른 예약이나 하우스키핑이 이미 존재하면 오류 메시지를 출력한다.

