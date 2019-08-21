package app.configuration

import app.domain.DecryptedStream
import app.domain.EncryptedStream
import app.exceptions.DataKeyDecryptionException
import app.exceptions.WriterException
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.batch.core.launch.support.RunIdIncrementer
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.ItemWriter
import org.springframework.batch.item.support.CompositeItemProcessor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@EnableBatchProcessing
@Profile("batchRun")
class JobConfiguration {

    @Bean
    fun importUserJob(step: Step) =
            jobBuilderFactory.get("snapshotSenderJob")
                    .incrementer(RunIdIncrementer())
                    .flow(step)
                    .end()
                    .build()

    @Bean
    fun step() =
            stepBuilderFactory.get("step")
                    .chunk<EncryptedStream, DecryptedStream>(10)
                    .reader(itemReader)
                    .faultTolerant()
                    .skip(DataKeyDecryptionException::class.java)
                    .skip(WriterException::class.java)
                    .skipLimit(Integer.MAX_VALUE)
                    .processor(itemProcessor())
                    .writer(itemWriter)
                    .build()

    fun itemProcessor(): ItemProcessor<EncryptedStream, DecryptedStream> =
            CompositeItemProcessor<EncryptedStream, DecryptedStream>().apply {
                setDelegates(listOf(datakeyProcessor, decryptionProcessor))
            }

    @Autowired
    lateinit var itemReader: ItemReader<EncryptedStream>

    @Autowired
    lateinit var datakeyProcessor: ItemProcessor<EncryptedStream, EncryptedStream>

    @Autowired
    lateinit var decryptionProcessor: ItemProcessor<EncryptedStream, DecryptedStream>

    @Autowired
    lateinit var itemWriter: ItemWriter<DecryptedStream>

    @Autowired
    lateinit var jobBuilderFactory: JobBuilderFactory

    @Autowired
    lateinit var stepBuilderFactory: StepBuilderFactory
}