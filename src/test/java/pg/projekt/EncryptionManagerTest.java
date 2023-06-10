package pg.projekt;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

/**
 * Unit test for simple App.
 */
public class EncryptionManagerTest
{
    private EncryptionManager em;

    @Before
    public void init(){
        em = new EncryptionManager();

    }
    @Test
    public void aesEncryptionCBCTest()
    {

        em.generateAndSetSessionKey();
        byte[] iv  = EncryptionManager.generateRandomBytes(16);



        String data = "Zaszyfrowana wiadomosc";

        byte[] enc = EncryptionManager.encryptAES(data.getBytes(), em.getSessionKey(), iv, CipherMode.CBC);

        String result = new String(enc, StandardCharsets.UTF_8);

        System.out.print("Szyfrogram: ");
        System.out.println(result);


        assertFalse(data.equals(result));

    }

    @Test
    public void aesDecryptionCBCTest()
    {

        em.generateAndSetSessionKey();
        byte[] iv  = EncryptionManager.generateRandomBytes(16);



        String data = "Zaszyfrowana wiadomosc asd           asedqwr qwbqeqeqwe asd asdasdasdasdouyaspidua psdi asdpoa uspodojuaspdujaso[djua[osdu[aod[oasudu";

        byte[] enc = EncryptionManager.encryptAES(data.getBytes(), em.getSessionKey(), iv, CipherMode.CBC);

        byte[] dec = EncryptionManager.decryptAES(enc, em.getSessionKey(), iv, CipherMode.CBC);

        String result = new String(dec, StandardCharsets.UTF_8);
        System.out.println("Teskt przed zaszyfrowaniem: "+ data);

        String enc_s = new String(enc, StandardCharsets.UTF_8);
        System.out.print("Szyfrogram: ");
        System.out.println(enc_s);
        System.out.println("Tekst po odszyfrowaniu: "+ result);


        assertTrue(data.equals(result));

    }


    @Test
    public void aesEncryptionECBTest()
    {

        em.generateAndSetSessionKey();
        byte[] iv  = EncryptionManager.generateRandomBytes(16);



        String data = "Zaszyfrowana wiadomosc";

        byte[] enc = EncryptionManager.encryptAES(data.getBytes(), em.getSessionKey(), iv, CipherMode.ECB);



        String result = new String(enc, StandardCharsets.UTF_8);
        System.out.print("Szyfrogram: ");
        System.out.println(result);


        assertFalse(data.equals(result));

    }

    @Test
    public void aesDecryptionECBTest()
    {

        em.generateAndSetSessionKey();
        byte[] iv  = EncryptionManager.generateRandomBytes(16);



        String data = "Zaszyfrowana wiadomosc asd              asdasdasfasgasgasgasdasdasfass";

        byte[] enc = EncryptionManager.encryptAES(data.getBytes(), em.getSessionKey(), iv, CipherMode.ECB);

        byte[] dec = EncryptionManager.decryptAES(enc, em.getSessionKey(), iv, CipherMode.ECB);

        String result = new String(dec, StandardCharsets.UTF_8);
        System.out.println("Teskt przed zaszyfrowaniem: "+ data);

        String enc_s = new String(enc, StandardCharsets.UTF_8);
        System.out.print("Szyfrogram: ");
        System.out.println(enc_s);
        System.out.println("Tekst po odszyfrowaniu: "+ result);


        assertTrue(data.equals(result));

    }
}
