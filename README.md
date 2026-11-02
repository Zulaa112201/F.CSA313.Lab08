![Tests](https://github.com/Zulaa112201/F.CSA313.Lab08/actions/workflows/test.yml/badge.svg)

# Лаборатори №8 — Mock объектууд (Mockito)

**F.CSA313 — Программ хангамжийн чанарын баталгаа ба тест (2026)**

- **Оюутны нэр:** <Э.Номинзул>
- **Оюутны код:** <B242270082>

## Орчин

`java -version`:

```
openjdk version "21.0.10-internal" 2026-01-20
OpenJDK Runtime Environment (build 21.0.10-internal-adhoc.rattler.src)
OpenJDK 64-Bit Server VM (build 21.0.10-internal-adhoc.rattler.src, mixed mode, sharing)
```

`mvn -version`:

```
Apache Maven 3.9.9
Maven home: /usr/share/maven
Java version: 21.0.10-internal, vendor: N/A, runtime: /home/nominzul/miniforge3/lib/jvm
Default locale: en_US, platform encoding: UTF-8
OS name: "linux", version: "6.12.107+deb13-amd64", arch: "amd64", family: "unix"
```



## Тестийн тоо

`results/mvn-test.txt`-ийн сүүлийн гаралт: **Tests run: 10, Failures: 0, Errors: 0, Skipped: 0**, `BUILD SUCCESS`.
10 тест = 7 энгийн тест (A1–A7) + A8-ийн 3 мөр (параметржүүлсэн тест мөр бүр тусдаа тоологдоно).

## A8 дахь `lenient()`

A8-д мөр бүр (NO_STUDENT, INACTIVE, PREREQ) өөр өөр stub тохируулдаг бөгөөд тухайн мөрд хэрэггүй stub (жишээ нь NO_STUDENT мөрд `courseExists`) ашиглагдахгүй. Mockito-гийн strict stubs горим ашиглагдаагүй stub бүрийг `UnnecessaryStubbingException`-ээр унагаадаг тул тэдгээрийг `lenient().when(...)`-ээр зарласан.

## Хэсэг B — Мини мутаци

Мутант бүрийн дараа код эх хэлбэрт нь сэргээгдсэн. Гаралтууд `results/mvn-test-mutant-N.txt` файлуудад байгаа.

| # | Мутант (зориуд эвдсэн зүйл) | Унасан тестүүд | Тайлбар | Үр дүн |
|---|------------------------------|----------------|---------|--------|
| 1 | `if (!student.active()) return "ERROR_INACTIVE_STUDENT";` комментлосон | A3 `register_inactiveStudent_returnsInactiveAndNeverTouchesEmailSender`; A8 `register_errorScenario_returnsExpectedCodeAndNeverSaves[2]` (INACTIVE мөр) | Шалгалтгүй болоход алгоритм цааш явж `ERROR_NO_COURSE` буцаасан (хүлээсэн: `ERROR_INACTIVE_STUDENT`) | Failures: 2, BUILD FAILURE |
| 2 | `repository.saveRegistration(studentId, courseId);` комментлосон | A1 `register_activeStudentWithPrerequisitesMet_returnsOkSavesAndSendsEmail`; A5 `register_emailSenderThrows_stillReturnsOkAndSavesRegistration` | `verify(repository).saveRegistration(...)` — "Wanted but not invoked" | Failures: 2, BUILD FAILURE |
| 3 | `if (studentId == null \|\| courseId == null)` → `if (studentId == null)` | A6 `register_nullArguments_throwsIllegalArgumentAndTouchesNoDependency` | `register("S1", null)` үед `IllegalArgumentException` шидэгдээгүй | Failures: 1, BUILD FAILURE |

### Мутацийн дүгнэлт

Гурван мутант бүгд баригдсан, амьд үлдсэн мутант байхгүй. Мутант 3-ыг A6 барьж чадсан шалтгаан нь тест хоёр аргумент тус бүрийн `null`-ийг тусад нь (`(null, "CS101")`, `("S1", null)`) шалгадаг явдал юм; хэрэв зөвхөн `(null, null)`-ийг шалгасан бол энэ мутант амьд үлдэх байсан. Мутант 2-т A7 (ArgumentCaptor) унаагүй нь A7 зөвхөн и-мэйлийн биеийг шалгадаг, хадгалалтыг шалгадаггүйтэй холбоотой — тест бүр өөр өөр зүйлийг хамгаалдаг. Мутаци хийх үед `UnnecessaryStubbingException` гараагүй.

## Нэмэлт — CI (GitHub Actions)

`.github/workflows/test.yml` нэмсэн бөгөөд push бүрд `mvn test` үүлэнд (JDK 17) ажилладаг. Дээрх badge-ээс төлөвийг харж болно.

## Дүгнэлт

Mock нь тестийн үед хамаарлын үүргийг тоглодог, дуудалтуудыг тэмдэглэж, дараа нь `verify`-аар шалгах боломж олгодог объект бол гараар бичсэн fake нь жинхэнэ логик (жишээ нь санах ойн Map) агуулсан, тестээс тестэд дахин ашиглагддаг хялбаршуулсан хэрэгжүүлэлт юм. Mock-ийг ашиглавал `StudentRepository`, `EmailSender` шиг интерфейсийн хэрэгжүүлэлт огт бичихгүйгээр нэг тестэд хэрэгтэй зан үйлийг л хурдан тохируулж болдог. Харин `Student` шиг энгийн өгөгдлийн record, `String`, `List` зэрэг хамаарал багатай утгын объектуудыг mock хийх ёсгүй, учир нь тэдгээрийг шууд үүсгэх нь илүү энгийн бөгөөд найдвартай. Мөн тестлэж буй классаа өөрийг нь mock хийж болохгүй, учир нь тэгвэл бодит код биш mock-ийг тестэлсэн болно. `verify`-г хэтрүүлж ашиглавал (жишээ нь дотоод дуудалтын дараалал, тоог бүгдийг нь шалгавал) тест хэрэгжүүлэлтийн нарийн мэдээллээс хэт хамааралтай болж, зан төлөв өөрчлөгдөөгүй рефакторинг хийхэд ч тест унадаг. Тиймээс гол үр дүн (буцаасан утга, чухал гаж нөлөө: хадгалалт, и-мэйл) болон "хэзээ ч болохгүй зүйл" (`never()`)-ийг л шалгах нь зөв. Хэсэг B-ийн мутаци нь тестүүд үнэхээр ажиллаж байгааг батлах энгийн арга бөгөөд тест ногоон гэдэг нь тест сайн гэсэн үг биш гэдгийг харуулсан.

---


