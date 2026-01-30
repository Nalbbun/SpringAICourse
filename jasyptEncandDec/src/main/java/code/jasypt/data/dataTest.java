package code.jasypt.data;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;

public class dataTest {

	public static void main(String[] args) {
		StandardPBEStringEncryptor jasypt = new StandardPBEStringEncryptor();
		jasypt.setPassword("SanghoAPIKey"); // 복호화에 사용할 마스터 키
		jasypt.setAlgorithm("PBEWithMD5AndDES");// 알고리즘 명시

		String data ="TEST_KEY"; // 복호화 할 데이터
		System.out.println("data(" + data + ")");
		
		String encrypted = jasypt.encrypt(data);
		System.out.println("ENC(" + encrypted + ")");
		
		String decrypted = jasypt.decrypt(encrypted);
		System.out.println("DEC(" + decrypted + ")");
	}

}
