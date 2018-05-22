package br.com.batch;

import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import br.com.batch.model.Pessoa;
import br.com.batch.model.PessoaItemProcessor;

@Configuration
@EnableBatchProcessing
public class BathConfiguration {

	@Autowired
	public JobBuilderFactory jobBuilderFactory;
	
	@Autowired
	public StepBuilderFactory stepBuilderFactory;
	
	@Autowired
	public DataSource dataSource;
	
	@Bean
	public DataSource dataSource() {
		final DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.mysql.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://localhost/etl?autoReconnect=true&useSSL=false");
		dataSource.setUsername("root");
		dataSource.setPassword("root");
		
		return dataSource;
	}
	
	@Bean
	public JdbcCursorItemReader<Pessoa> reader(){
		
		System.out.println("Iniciando reader");
		
		JdbcCursorItemReader<Pessoa> reader = new JdbcCursorItemReader<Pessoa>();
		reader.setDataSource(dataSource);
		reader.setSql("SELECT nome, idade, mae FROM pessoa");
		reader.setRowMapper(new PessoaRowMapper());
		
		return reader;
	}
	
	public class PessoaRowMapper implements RowMapper<Pessoa>{

		@Override
		public Pessoa mapRow(ResultSet rs, int rowNum) throws SQLException {
			
			Pessoa pessoa = new Pessoa();
			pessoa.setNome(rs.getString("nome"));
			pessoa.setIdade(rs.getInt("idade"));
			pessoa.setMae(rs.getString("mae"));
			
			System.out.println(pessoa);
			
			return pessoa;
		}
	}
	
	@Bean
	public PessoaItemProcessor processor() {
		return new PessoaItemProcessor();
	}
	
	@Bean
	public FlatFileItemWriter<Pessoa> writer(){
		FlatFileItemWriter<Pessoa> writer = new FlatFileItemWriter<Pessoa>();
		writer.setEncoding("UTF-8");
		writer.setResource(new FileSystemResource("c://teste//pessoas.csv"));
		writer.setLineAggregator(new DelimitedLineAggregator<Pessoa>(){{
				setDelimiter(",");
				setFieldExtractor(new BeanWrapperFieldExtractor<Pessoa>() {{
						setNames(new String[]{"nome","idade","mae"});
					}});
		}});
		
		
		return writer;
	}
	
	@Bean
	public Step step1() {
		return stepBuilderFactory.get("step1").<Pessoa,Pessoa>chunk(10)
				.reader(reader())
				.processor(processor())
				.writer(writer())
				.build();
	}
	
	@Bean
	public Job exportPessoaJob() {
		return jobBuilderFactory.get("exportPessoaJob")
				.incrementer(new RunIdIncrementer())
				.flow(step1())
				.end()
				.build();
				
	}
}

